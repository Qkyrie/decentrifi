@file:OptIn(ExperimentalTime::class)

package fi.decentri.dataingest.ingest

import arrow.fx.coroutines.parMap
import fi.decentri.abi.AbiService
import fi.decentri.abi.LogDecoder.decode
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
    private val web3jManager: Web3jManager,
    private val metadataRepository: IngestionMetadataRepository,
    private val eventRepository: EventRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val abiService = AbiService()
    private val blockService: BlockService = BlockService(Web3jManager.getInstance())

    /**
     * Ingest events for a contract
     */
    suspend fun ingest(contract: Contract) {
        log.info("Starting event log ingestion for contract ${contract.address}")

        val (_, _, eventBatchSize, pollingInterval, _) = web3jManager.getNetworkConfig(contract.chain)
            ?: error("unable to get network config for ${contract.chain}")

        // Parse ABI to extract events
        val (_, events) = abiService.parseABI(contract.abi)
        if (events.isEmpty()) {
            log.info("No events found in ABI for contract ${contract.address}. Skipping event ingestion.")
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

        log.info("Starting event ingestion from block $startBlock to target latest block $targetLatestBlock")

        var lastProcessedBlock = startBlock
        var completed = false

        // Process blocks in batches until we reach the target latest block
        while (!completed) {

            kotlin.runCatching {
                // Check if we've reached the target block
                if (lastProcessedBlock >= targetLatestBlock) {
                    log.info("Reached target latest block $targetLatestBlock. Event ingestion complete.")
                    completed = true
                } else {
                    // Calculate the next batch to process
                    val toBlock = minOf(lastProcessedBlock + eventBatchSize, targetLatestBlock)
                    log.info("Processing event logs for blocks ${lastProcessedBlock + 1} to $toBlock (${toBlock - lastProcessedBlock} blocks)")

                    // Process the block range to get events
                    processLogsInBlockRange(
                        contract.chain,
                        lastProcessedBlock + 1,
                        toBlock,
                        contract.address.lowercase(),
                        contract.abi
                    )

                    contract.updateMetadata(MetadataType.LAST_PROCESSED_BLOCK_EVENTS, toBlock.toString())
                    contract.updateMetadata(MetadataType.EVENTS_LAST_RUN_TIMESTAMP, Instant.now().toString())

                    lastProcessedBlock = toBlock

                    // Calculate and log progress
                    val progressPercentage =
                        ((lastProcessedBlock - startBlock).toDouble() / (targetLatestBlock - startBlock).toDouble() * 100).toInt()
                    log.info("Event ingestion progress: $progressPercentage% (processed up to block $lastProcessedBlock of $targetLatestBlock)")
                }
            }.onFailure {
                log.error("Error during event log ingestion: ${it.message}", it)
                delay(pollingInterval)
            }
        }

        log.info("Event ingestion run completed successfully. Processed blocks $startBlock to $targetLatestBlock")
    }


    private suspend fun Contract.updateMetadata(metadataType: MetadataType, value: String) {
        metadataRepository.updateMetadataForContractId(
            this.id!!,
            metadataType,
            value
        )
    }

    /**
     * Process logs within a block range
     */
    private suspend fun processLogsInBlockRange(
        network: String,
        from: Long,
        to: Long,
        address: String,
        abi: String
    ) {
        log.info("Fetching event logs from block $from to $to for contract $address")

        kotlin.runCatching {
            val logs = fetchLogs(from, to, address, network)

            if (logs.isEmpty()) {
                log.debug("No event logs found for the contract in blocks $from to $to")
                return
            }

            log.info("Found ${logs.size} event logs for the contract in blocks $from to $to")

            // Process each log to extract event data
            logs.chunked(1500).forEach { chunkedLogs ->
                val timed = measureTime {
                    val eventLogs = chunkedLogs.parMap(concurrency = 24) { log ->
                        try {
                            processLog(log, network, address, abi)
                        } catch (e: Exception) {
                            this@EventIngestorService.log.error("Error processing log: ${e.message}", e)
                            null
                        }
                    }.filterNotNull()
                    if (eventLogs.isNotEmpty()) {
                        eventRepository.batchInsertEvents(eventLogs)
                        log.debug("Saved ${eventLogs.size} event logs to database")
                    }
                }
                log.info("Processed ${chunkedLogs.size} logs in ${timed.inWholeSeconds} seconds")
            }
        }.onFailure {
            log.error("Error fetching event logs: ${it.message}", it)
            throw it
        }
    }

    private suspend fun fetchLogs(
        fromBlock: Long,
        toBlock: Long,
        contractAddress: String,
        network: String
    ): List<EthLog.LogObject> {
        // Create a filter for the contract address
        val filter = EthFilter(
            DefaultBlockParameter.valueOf(BigInteger.valueOf(fromBlock)),
            DefaultBlockParameter.valueOf(BigInteger.valueOf(toBlock)),
            contractAddress
        )

        // Get logs for the filter
        val logs =
            web3jManager.web3(network)!!.web3j.ethGetLogs(filter).sendAsync().await().logs as List<EthLog.LogObject>
        return logs
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
            val decodedLog = log.decode(abi)
            decoded = decodedLog?.parameters as Map<String, Any>?
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
}