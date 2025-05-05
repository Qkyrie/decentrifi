@file:OptIn(ExperimentalTime::class)

package fi.decentri.dataingest.ingest

import com.fasterxml.jackson.databind.JsonNode
import fi.decentri.block.BlockService
import fi.decentri.dataingest.config.EthereumConfig
import fi.decentri.dataingest.model.Contract
import fi.decentri.dataingest.repository.IngestionMetadataRepository
import fi.decentri.dataingest.repository.RawInvocationData
import fi.decentri.dataingest.repository.RawInvocationsRepository
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.time.ExperimentalTime

/**
 * Service responsible for blockchain data ingestion
 */
class IngestorService(
    private val config: EthereumConfig, private val web3j: Web3j
) {

    private val blockService = BlockService(web3j)
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val metadataRepository = IngestionMetadataRepository()
    private val rawInvocationsRepository = RawInvocationsRepository()

    suspend fun ingest(contract: Contract) {
        logger.info("Starting trace_filter data ingestion for contract $contract")
        // Get the latest block at the start of this run - this is our target end block
        val targetLatestBlock = web3j.ethBlockNumber().send().blockNumber.longValueExact()

        val startBlock =
            metadataRepository.getLastProcessedBlockForContract(contract.id!!) ?: blockService.getBlockClosestTo(
                LocalDateTime.now().minusHours(25)
            )

        logger.info("Starting ingestion from block $startBlock to target latest block $targetLatestBlock")

        var lastProcessedBlock = startBlock
        var completed = false

        // Process blocks in batches until we reach the target latest block
        while (!completed) {
            try {
                // Check if we've reached the target block
                if (lastProcessedBlock >= targetLatestBlock) {
                    logger.info("Reached target latest block $targetLatestBlock. Ingestion complete.")
                    completed = true
                } else {
                    // Calculate the next batch to process
                    val toBlock = minOf(lastProcessedBlock + config.batchSize, targetLatestBlock)
                    logger.info("Processing blocks ${lastProcessedBlock + 1} to $toBlock with trace_filter (${toBlock - lastProcessedBlock} blocks)")

                    // Process the block range using trace_filter
                    processBlockRangeWithTraceFilter(
                        contract.chain, lastProcessedBlock + 1, toBlock, contract.address.lowercase(Locale.getDefault())
                    )
                    lastProcessedBlock = toBlock

                    metadataRepository.updateLastProcessedBlockForContract(contract.id, toBlock)

                    // Calculate and log progress
                    val progressPercentage =
                        ((lastProcessedBlock - startBlock).toDouble() / (targetLatestBlock - startBlock).toDouble() * 100).toInt()
                    logger.info("Progress: $progressPercentage% (processed up to block $lastProcessedBlock of $targetLatestBlock)")
                }
            } catch (e: Exception) {
                logger.error("Error during trace_filter ingestion: ${e.message}", e)
                delay(config.pollingInterval)
            }

            // Add a small delay between batches to avoid overwhelming the node
            delay(100)
        }

        logger.info("Ingestion run completed successfully. Processed blocks $startBlock to $targetLatestBlock")
    }

    /**
     * Process a range of blocks using trace_filter to capture all transactions including internal ones
     */
    private suspend fun processBlockRangeWithTraceFilter(
        network: String, fromBlock: Long, toBlock: Long, toAddress: String
    ) {
        logger.info("Filtering traces from block $fromBlock to $toBlock for contract $toAddress")

        // Create trace_filter request parameters
        val traceFilterParams = mapOf(
            "fromBlock" to "0x${fromBlock.toString(16)}",
            "toBlock" to "0x${toBlock.toString(16)}",
            "toAddress" to listOf(toAddress)
        )

        try {
            // Execute trace_filter RPC call
            val traceFilterResponse = executeTraceFilter(traceFilterParams)
            val traces = traceFilterResponse.result

            if (traces.isEmpty()) {
                logger.debug("No traces found for the contract in blocks $fromBlock to $toBlock")
                return
            }

            logger.info("Found ${traces.size} traces for the contract in blocks $fromBlock to $toBlock")

            // Process each trace to extract invocation data
            val invocations = traces.mapNotNull { trace ->
                try {
                    // Extract transaction data from the trace
                    val txHash = trace.get("transactionHash")?.asText() ?: return@mapNotNull null
                    val blockNumber = trace.get("blockNumber")?.asText()?.let {
                        Numeric.decodeQuantity(it).longValueExact()
                    } ?: return@mapNotNull null

                    // Get the input data (function call data)
                    val input = trace.get("action")?.get("input")?.asText() ?: "0x"

                    // Extract function selector (first 4 bytes of input)
                    val functionSelector = if (input.length >= 10) {
                        input.substring(0, 10)
                    } else {
                        "0x"
                    }

                    // Get the sender address
                    val from = trace.get("action")?.get("from")?.asText() ?: return@mapNotNull null

                    // Get the block to extract timestamp
                    val block = web3j.ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(BigInteger(trace.get("blockNumber").asText())), false
                    ).send().block

                    // Get transaction receipt for status and gas used
                    val receipt = web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt.orElse(null)

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
                        gasUsed = receipt?.gasUsed?.longValueExact() ?: 0
                    )
                } catch (e: Exception) {
                    logger.error("Error processing trace: ${e.message}", e)
                    null
                }
            }.distinctBy { it.txHash } // Deduplicate by transaction hash

            // Store invocations in the database
            if (invocations.isNotEmpty()) {
                rawInvocationsRepository.batchInsert(invocations)
                logger.info("Saved ${invocations.size} invocations to database from trace_filter")
            }

        } catch (e: Exception) {
            logger.error("Error executing trace_filter: ${e.message}", e)
            throw e
        }
    }

    /**
     * Execute trace_filter RPC call
     * The trace_filter method is an Ethereum JSON-RPC API endpoint that returns transaction traces
     */
    private fun executeTraceFilter(params: Map<String, Any>): TraceFilterResponse {
        val request = Request<Any, TraceFilterResponse>(
            "trace_filter", listOf(params), HttpService(config.rpcUrl), TraceFilterResponse::class.java
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
