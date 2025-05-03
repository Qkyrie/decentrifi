package fi.decentri.dataingest.ingest

import com.fasterxml.jackson.databind.JsonNode
import fi.decentri.dataingest.config.EthereumConfig
import fi.decentri.dataingest.repository.IngestionMetadataRepository
import fi.decentri.dataingest.repository.RawInvocationData
import fi.decentri.dataingest.repository.RawInvocationsRepository
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.EthLog
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.time.Instant
import java.util.*

/**
 * Service responsible for blockchain data ingestion
 */
class IngestorService(private val config: EthereumConfig) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val web3j: Web3j = Web3j.build(HttpService(config.rpcUrl))
    private val metadataRepository = IngestionMetadataRepository()
    private val rawInvocationsRepository = RawInvocationsRepository()

    /**
     * Start the blockchain data ingestion process using trace_filter for all transactions
     */
    suspend fun ingest() {
        logger.info("Starting trace_filter data ingestion for contract ${config.contractAddress}")

        var lastProcessedBlock = if (config.startBlock > 0) {
            config.startBlock
        } else {
            metadataRepository.getLastProcessedBlock()
        }

        logger.info("Last processed block with trace_filter: $lastProcessedBlock")

        while (true) {
            try {
                // Get the latest block number
                val latestBlock = web3j.ethBlockNumber().send().blockNumber.longValueExact()

                if (lastProcessedBlock >= latestBlock) {
                    logger.debug("No new blocks to process. Current: $lastProcessedBlock, Latest: $latestBlock")
                    delay(config.pollingInterval)
                    continue
                }

                // Calculate the range to process
                val toBlock = minOf(lastProcessedBlock + config.batchSize, latestBlock)
                logger.info("Processing blocks $lastProcessedBlock to $toBlock with trace_filter")

                // Process the block range using trace_filter
                processBlockRangeWithTraceFilter(
                    lastProcessedBlock + 1,
                    toBlock,
                    config.contractAddress.lowercase(Locale.getDefault())
                )

                // Update the last processed block
                lastProcessedBlock = toBlock
                metadataRepository.updateLastProcessedBlock(lastProcessedBlock)

            } catch (e: Exception) {
                logger.error("Error during trace_filter ingestion: ${e.message}", e)
                delay(config.pollingInterval)
            }

            // Wait before the next polling cycle
            delay(config.pollingInterval)
        }
    }

    /**
     * Process a range of blocks to extract and store contract invocations
     */
    private suspend fun processBlockRange(fromBlock: Long, toBlock: Long) {
        // Create a filter for Transfer events
        val filter = EthFilter(
            DefaultBlockParameter.valueOf(BigInteger.valueOf(fromBlock)),
            DefaultBlockParameter.valueOf(BigInteger.valueOf(toBlock)),
            config.contractAddress
        )

        // Get all Transfer events in the range
        val transferEvents = web3j.ethGetLogs(filter).send().logs

        if (transferEvents.isEmpty()) {
            logger.debug("No transfer events found in blocks $fromBlock to $toBlock")
            return
        }

        logger.info("Found ${transferEvents.size} transfer events in blocks $fromBlock to $toBlock")

        // Process each transaction to extract invocation data
        val invocations = transferEvents.mapNotNull { logResult ->
            try {
                val log = logResult.get() as EthLog.LogObject
                val txHash = log.transactionHash
                val blockNumber = log.blockNumber.longValueExact()

                // Get the transaction
                val tx = web3j.ethGetTransactionByHash(txHash).send().transaction.orElse(null) ?: return@mapNotNull null

                // Get the transaction receipt
                val receipt = web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt.orElse(null)
                    ?: return@mapNotNull null

                // Get the block to extract timestamp
                val block = web3j.ethGetBlockByNumber(
                    DefaultBlockParameter.valueOf(log.blockNumber), false
                ).send().block

                // Extract the function selector (first 4 bytes of input data)
                val inputData = tx.input
                val functionSelector = if (inputData.length >= 10) {
                    inputData.substring(0, 10)
                } else {
                    "0x"
                }

                // Create invocation data
                RawInvocationData(
                    network = "ethereum",
                    contractAddress = config.contractAddress,
                    blockNumber = blockNumber,
                    blockTimestamp = Instant.ofEpochSecond(block.timestamp.longValueExact()),
                    txHash = txHash,
                    fromAddress = tx.from,
                    functionSelector = functionSelector,
                    inputArgs = mapOf("rawInput" to inputData),
                    status = receipt.isStatusOK,
                    gasUsed = receipt.gasUsed.longValueExact()
                )
            } catch (e: Exception) {
                logger.error("Error processing transaction from log: ${e.message}", e)
                null
            }
        }

        // Store invocations in the database
        if (invocations.isNotEmpty()) {
            rawInvocationsRepository.batchInsert(invocations)
            logger.info("Saved ${invocations.size} invocations to database")
        }
    }

    /**
     * Process a range of blocks using trace_filter to capture all transactions including internal ones
     */
    private suspend fun processBlockRangeWithTraceFilter(
        fromBlock: Long,
        toBlock: Long,
        toAddress: String
    ) {
        logger.info("Filtering traces from block $fromBlock to $toBlock for contract ${config.contractAddress}")

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
                        DefaultBlockParameter.valueOf(BigInteger(trace.get("blockNumber").asText())),
                        false
                    ).send().block

                    // Get transaction receipt for status and gas used
                    val receipt = web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt.orElse(null)

                    // Create invocation data
                    RawInvocationData(
                        network = "ethereum",
                        contractAddress = config.contractAddress,
                        blockNumber = blockNumber,
                        blockTimestamp = Instant.ofEpochSecond(block.timestamp.longValueExact()),
                        txHash = txHash,
                        fromAddress = from,
                        functionSelector = functionSelector,
                        inputArgs = mapOf("rawInput" to input),
                        status = receipt?.isStatusOK ?: false,
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
            "trace_filter",
            listOf(params),
            HttpService(config.rpcUrl),
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
