@file:OptIn(ExperimentalTime::class, ExperimentalTime::class)

package fi.decentri.application.usecases

import arrow.core.Either
import arrow.core.getOrElse
import arrow.fx.coroutines.parMap
import com.fasterxml.jackson.databind.JsonNode
import fi.decentri.application.ports.BlockPort
import fi.decentri.application.ports.IngestionMetadataPort
import fi.decentri.dataingest.config.Web3jManager
import fi.decentri.dataingest.model.Contract
import fi.decentri.dataingest.model.MetadataType
import fi.decentri.infrastructure.repository.ingestion.RawInvocationData
import fi.decentri.infrastructure.repository.ingestion.RawInvocationRepository
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.utils.Numeric
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.time.ExperimentalTime

/**
 * Service responsible for blockchain raw invocation data ingestion
 */
class IngestRawInvocationsUseCase(
    private val web3jManager: Web3jManager,
    private val metadata: IngestionMetadataPort,
    private val rawInvocationsRepository: RawInvocationRepository,
    private val blocks: BlockPort,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    suspend fun invoke(contract: Contract) {
        val config = web3jManager.getNetworkConfig(contract.chain) ?: throw IllegalArgumentException(
            "Network configuration not found for network: ${contract.chain}"
        )
        log.info("Starting trace_filter data ingestion for contract $contract")
        // Get the latest block at the start of this run - this is our target end block
        val targetLatestBlock = blocks.getLatestBlock(contract.chain)

        val startBlock =
            metadata.getMetadatForContractId(MetadataType.LAST_PROCESSED_BLOCK_RAW_INVOCATIONS, contract.id!!)
                ?.toLongOrNull() ?: blocks.getBlockClosestTo(
                LocalDateTime.now().minusHours(25),
                contract.chain
            )

        log.info("Starting ingestion from block $startBlock to target latest block $targetLatestBlock")

        var lastProcessedBlock = startBlock
        var completed = false

        // Process blocks in batches until we reach the target latest block
        while (!completed) {
            try {
                // Check if we've reached the target block
                if (lastProcessedBlock >= targetLatestBlock) {
                    log.info("Reached target latest block $targetLatestBlock. Ingestion complete.")
                    completed = true
                } else {
                    // Calculate the next batch to process
                    val toBlock = minOf(lastProcessedBlock + config.batchSize, targetLatestBlock)
                    log.info("Processing blocks ${lastProcessedBlock + 1} to $toBlock with trace_filter (${toBlock - lastProcessedBlock} blocks)")

                    // Process the block range using trace_filter
                    processBlockRangeWithTraceFilter(
                        contract.chain, lastProcessedBlock + 1, toBlock, contract.address.lowercase(Locale.getDefault())
                    )


                    contract.updateMetadata(MetadataType.LAST_PROCESSED_BLOCK_RAW_INVOCATIONS, toBlock.toString())
                    contract.updateMetadata(MetadataType.RAW_INVOCATIONS_LAST_RUN_TIMESTAMP, Instant.now().toString())


                    lastProcessedBlock = toBlock

                    // Calculate and log progress
                    val progressPercentage =
                        ((lastProcessedBlock - startBlock).toDouble() / (targetLatestBlock - startBlock).toDouble() * 100).toInt()
                    log.info("Progress: $progressPercentage% (processed up to block $lastProcessedBlock of $targetLatestBlock)")
                }
            } catch (e: Exception) {
                log.error("Error during trace_filter ingestion: ${e.message}", e)
                delay(config.pollingInterval)
            }
        }

        log.info("Ingestion run completed successfully. Processed blocks $startBlock to $targetLatestBlock")
    }

    private suspend fun Contract.updateMetadata(metadataType: MetadataType, value: String) {
        metadata.updateMetadataForContractId(
            this.id!!,
            metadataType,
            value
        )
    }

    private suspend fun processBlockRangeWithTraceFilter(
        network: String, fromBlock: Long, toBlock: Long, toAddress: String
    ) {
        log.info("Filtering traces from block $fromBlock to $toBlock for contract $toAddress")


        try {
            val traces = getTraces(fromBlock, toBlock, toAddress, network)

            if (traces.isEmpty()) {
                log.debug("No traces found for the contract in blocks $fromBlock to $toBlock")
                return
            }

            log.info("Found ${traces.size} traces for the contract in blocks $fromBlock to $toBlock")

            // Process each trace to extract invocation data
            val invocations = traces.parMap(concurrency = 8) { trace ->
                try {
                    // Extract transaction data from the trace
                    val txHash = trace.get("transactionHash")?.asText() ?: return@parMap null
                    val blockNumber = trace.get("blockNumber")?.asText()?.let {
                        Numeric.decodeQuantity(it).longValueExact()
                    } ?: return@parMap null

                    // Get the input data (function call data)
                    val input = trace.get("action")?.get("input")?.asText() ?: "0x"

                    // Extract function selector (first 4 bytes of input)
                    val functionSelector = if (input.length >= 10) {
                        input.substring(0, 10)
                    } else {
                        "0x"
                    }

                    // Get the sender address
                    val from = trace.get("action")?.get("from")?.asText() ?: return@parMap null

                    val gasUsed = Either.catch {
                        Numeric.decodeQuantity(trace.get("result").get("gasUsed").textValue())
                    }.mapLeft {
                        log.error("Error decoding gasUsed: ${it.message}", it)
                        BigInteger.ZERO
                    }.getOrElse { BigInteger.ZERO }

                    // Get the block to extract timestamp
                    val block = blocks.getBlockByNumber(BigInteger(trace.get("blockNumber").asText()), network)

                    // Create invocation data
                    RawInvocationData(
                        network = network,
                        contractAddress = toAddress,
                        blockNumber = blockNumber,
                        blockTimestamp = Instant.ofEpochSecond(block.timestamp.longValueExact()),
                        txHash = txHash,
                        fromAddress = from,
                        functionSelector = functionSelector,
                        inputArgs = mapOf("rawInput" to input),
                        gasUsed = gasUsed.toLong()
                    )
                } catch (e: Exception) {
                    log.error("Error processing trace: ${e.message}", e)
                    null
                }
            }.filterNotNull().distinctBy { it.txHash } // Deduplicate by transaction hash

            // Store invocations in the database
            if (invocations.isNotEmpty()) {
                rawInvocationsRepository.batchInsert(invocations)
                log.info("Saved ${invocations.size} invocations to database from trace_filter")
            }

        } catch (e: Exception) {
            log.error("Error executing trace_filter: ${e.message}", e)
            throw e
        }
    }

    private suspend fun getTraces(
        fromBlock: Long,
        toBlock: Long,
        toAddress: String,
        network: String
    ): List<JsonNode> {
        val traceFilterParams = mapOf(
            "fromBlock" to "0x${fromBlock.toString(16)}",
            "toBlock" to "0x${toBlock.toString(16)}",
            "toAddress" to listOf(toAddress)
        )

        // Execute trace_filter RPC call
        val traceFilterResponse = executeTraceFilter(traceFilterParams, web3jManager.web3(network)!!)
        val traces = traceFilterResponse.result
        return traces
    }

    /**
     * Execute trace_filter RPC call
     * The trace_filter method is an Ethereum JSON-RPC API endpoint that returns transaction traces
     */
    private fun executeTraceFilter(params: Map<String, Any>, web3: Web3jManager.Web3): TraceFilterResponse {
        val request = Request<Any, TraceFilterResponse>(
            "trace_filter",
            listOf(params),
            web3.httpService,
            TraceFilterResponse::class.java
        )

        return request.send()
    }

    /**
     * Response class for trace_filter method
     */
    class TraceFilterResponse : Response<List<JsonNode>>() {
        override fun setResult(result: List<JsonNode>) {
            super.setResult(result)
        }
    }
}