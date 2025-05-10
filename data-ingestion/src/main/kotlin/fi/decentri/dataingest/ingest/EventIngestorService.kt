@file:OptIn(ExperimentalTime::class)

package fi.decentri.dataingest.ingest

import arrow.fx.coroutines.parMap
import fi.decentri.abi.AbiEvent
import fi.decentri.abi.AbiService
import fi.decentri.abi.DecodedLog
import fi.decentri.abi.LogDecoder
import fi.decentri.block.BlockService
import fi.decentri.dataingest.config.Web3jManager
import fi.decentri.dataingest.model.Contract
import fi.decentri.dataingest.model.MetadataType
import fi.decentri.dataingest.repository.EventLogData
import fi.decentri.dataingest.repository.EventRepository
import fi.decentri.dataingest.repository.IngestionMetadataRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.EthLog
import org.web3j.protocol.core.methods.response.Log
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

/**
 * Service responsible for blockchain event logs ingestion
 */
class EventIngestorService(
    private val web3jManager: Web3jManager
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val metadataRepository = IngestionMetadataRepository()
    private val eventRepository = EventRepository()
    private val abiService = AbiService()
    private val blockService: BlockService = BlockService(Web3jManager.getInstance())

    /**
     * Ingest events for a contract
     */
    suspend fun ingest(contract: Contract) {
        logger.info("Starting event log ingestion for contract ${contract.address}")

        val config = web3jManager.getNetworkConfig(contract.chain) ?: throw IllegalArgumentException()

        // Parse ABI to extract events
        val (_, events) = abiService.parseABI(contract.abi)
        if (events.isEmpty()) {
            logger.info("No events found in ABI for contract ${contract.address}. Skipping event ingestion.")
            return
        }

        // Get the latest block at the start of this run - this is our target end block
        val targetLatestBlock = blockService.getLatestBlock(contract.chain)

        // Get the last processed block or start from 24 hours ago
        val startBlock =
            metadataRepository.getMetadatForContractId(MetadataType.LAST_PROCESSED_BLOCK_EVENTS, contract.id!!)
                ?.toLongOrNull()
                ?: blockService.getBlockClosestTo(
                    LocalDateTime.now().minusHours(24),
                    contract.chain
                )

        logger.info("Starting event ingestion from block $startBlock to target latest block $targetLatestBlock")

        var lastProcessedBlock = startBlock
        var completed = false

        // Process blocks in batches until we reach the target latest block
        while (!completed) {
            try {
                // Check if we've reached the target block
                if (lastProcessedBlock >= targetLatestBlock) {
                    logger.info("Reached target latest block $targetLatestBlock. Event ingestion complete.")
                    completed = true
                } else {
                    // Calculate the next batch to process
                    val toBlock = minOf(lastProcessedBlock + config.eventBatchSize, targetLatestBlock)
                    logger.info("Processing event logs for blocks ${lastProcessedBlock + 1} to $toBlock (${toBlock - lastProcessedBlock} blocks)")

                    // Process the block range to get events
                    processLogsInBlockRange(
                        contract.chain,
                        lastProcessedBlock + 1,
                        toBlock,
                        contract.address.lowercase(Locale.getDefault()),
                        contract.abi
                    )


                    updateLastBlock(contract, toBlock)
                    updateLastTimestamp(contract)

                    lastProcessedBlock = toBlock

                    // Calculate and log progress
                    val progressPercentage =
                        ((lastProcessedBlock - startBlock).toDouble() / (targetLatestBlock - startBlock).toDouble() * 100).toInt()
                    logger.info("Event ingestion progress: $progressPercentage% (processed up to block $lastProcessedBlock of $targetLatestBlock)")
                }
            } catch (e: Exception) {
                logger.error("Error during event log ingestion: ${e.message}", e)
                delay(config.pollingInterval)
            }
        }

        logger.info("Event ingestion run completed successfully. Processed blocks $startBlock to $targetLatestBlock")
    }

    private suspend fun updateLastTimestamp(contract: Contract) {
        metadataRepository.updateMetadataForContractId(
            contract.id!!,
            MetadataType.EVENTS_LAST_RUN_TIMESTAMP,
            Instant.now().toString()
        )
    }

    private suspend fun updateLastBlock(contract: Contract, toBlock: Long) {
        metadataRepository.updateMetadataForContractId(
            contract.id!!,
            MetadataType.LAST_PROCESSED_BLOCK_EVENTS,
            toBlock.toString()
        )
    }

    /**
     * Process logs within a block range
     */
    private suspend fun processLogsInBlockRange(
        network: String,
        fromBlock: Long,
        toBlock: Long,
        contractAddress: String,
        abi: String
    ) {
        logger.info("Fetching event logs from block $fromBlock to $toBlock for contract $contractAddress")

        try {
            // Create a filter for the contract address
            val filter = EthFilter(
                DefaultBlockParameter.valueOf(BigInteger.valueOf(fromBlock)),
                DefaultBlockParameter.valueOf(BigInteger.valueOf(toBlock)),
                contractAddress
            )

            // Get logs for the filter
            val logs =
                web3jManager.web3(network)!!.web3j.ethGetLogs(filter).sendAsync().await().logs as List<EthLog.LogObject>

            if (logs.isEmpty()) {
                logger.debug("No event logs found for the contract in blocks $fromBlock to $toBlock")
                return
            }

            logger.info("Found ${logs.size} event logs for the contract in blocks $fromBlock to $toBlock")

            // Process each log to extract event data
            logs.chunked(1500).forEach { chunkedLogs ->
                val timed = measureTime {
                    val eventLogs = chunkedLogs.parMap(concurrency = 24) { log ->
                        try {
                            processLog(log, network, contractAddress, abi)
                        } catch (e: Exception) {
                            logger.error("Error processing log: ${e.message}", e)
                            null
                        }
                    }.filterNotNull()
                    if (eventLogs.isNotEmpty()) {
                        eventRepository.batchInsertEvents(eventLogs)
                        logger.debug("Saved ${eventLogs.size} event logs to database")
                    }
                }
                logger.info("Processed ${chunkedLogs.size} logs in ${timed.inWholeSeconds} seconds")
            }
        } catch (e: Exception) {
            logger.error("Error fetching event logs: ${e.message}", e)
            throw e
        }
    }

    /**
     * Process a single log entry
     */
    private suspend fun processLog(
        log: Log,
        network: String,
        contractAddress: String,
        abi: String
    ): EventLogData? {
        // Extract basic log information
        val txHash = log.transactionHash ?: return null
        val logIndex = log.logIndex?.intValueExact() ?: 0
        val blockNumber = log.blockNumber?.longValueExact() ?: return null
        val topics = log.topics ?: emptyList()
        val data = log.data ?: "0x"

        // Get the block to extract timestamp
        val block = blockService.getBlockByNumber(log.blockNumber, network)
        val blockTimestamp = Instant.ofEpochSecond(block.timestamp.longValueExact())

        // Get the topic0 (event signature)
        val topic0 = if (topics.isNotEmpty()) topics[0] else null

        // Try to decode the event if we have the signature
        var eventName: String? = null
        var decoded: Map<String, Any>? = null

        if (topic0 != null) {
            val decodedLog = decodeEventData(log, abi)
            decoded = decodedLog?.parameters?.takeIf { it.values != null } as Map<String, Any>?
            eventName = decodedLog?.eventName
        }

        return EventLogData(
            network = network,
            contractAddress = contractAddress,
            txHash = txHash,
            logIndex = logIndex,
            blockNumber = blockNumber,
            blockTimestamp = blockTimestamp,
            topic0 = topic0,
            topics = topics,
            data = data,
            eventName = eventName,
            decoded = decoded
        )
    }

    /**
     * Decode event data using the ABI
     */
    private fun decodeEventData(
        log: Log,
        abi: String,
    ): DecodedLog? {
        try {
            // Use LogDecoder to decode the event data
            val decoded = LogDecoder.decodeLog(log, abi)

            return decoded ?: run {
                logger.debug("LogDecoder returned empty map for event, falling back to simple decoding")
                null
            }

        } catch (e: Exception) {
            logger.error("Error decoding event data with LogDecoder: ${e.message}", e)
            // Fallback to simpler decoding if LogDecoder throws an exception
            return null
        }
    }

    /**
     * Fallback method for decoding event data using a simpler approach
     */
    private fun fallbackDecodeEventData(
        eventAbi: AbiEvent,
        topics: List<String>,
        data: String
    ): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        try {
            // Create parameter types for indexed and non-indexed parameters
            val indexedParams = eventAbi.inputs.filter { it.indexed }
            val nonIndexedParams = eventAbi.inputs.filter { !it.indexed }

            // Decode indexed parameters from topics (skipping the first topic which is the event signature)
            if (indexedParams.isNotEmpty() && topics.size > 1) {
                for (i in indexedParams.indices) {
                    if (i + 1 < topics.size) {
                        val param = indexedParams[i]
                        // For simplicity, we'll just store the raw topic value
                        result[param.name] = topics[i + 1]
                    }
                }
            }

            // Decode non-indexed parameters from data
            if (nonIndexedParams.isNotEmpty() && data != "0x") {
                result["_data"] = data
            }

            return result
        } catch (e: Exception) {
            logger.error("Error in fallback decode: ${e.message}", e)
            return mapOf("_raw" to data)
        }
    }
}