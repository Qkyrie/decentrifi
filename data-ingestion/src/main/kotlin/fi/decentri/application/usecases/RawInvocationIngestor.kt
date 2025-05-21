@file:OptIn(ExperimentalTime::class, ExperimentalTime::class)

package fi.decentri.application.usecases

import arrow.fx.coroutines.parMap
import com.fasterxml.jackson.databind.JsonNode
import fi.decentri.application.ports.BlockPort
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
import java.util.*
import kotlin.time.ExperimentalTime

/**
 * Service responsible for blockchain raw invocation data ingestion
 */
class RawInvocationIngestor(
    private val web3jManager: Web3jManager,
    private val rawInvocationsRepository: RawInvocationRepository,
    private val blocks: BlockPort,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    suspend fun ingest(
        contract: Contract,
        startBlock: Long,
        endBlock: Long,
    ) {
        val config = web3jManager.getNetworkConfig(contract.chain) ?: throw IllegalArgumentException(
            "Network configuration not found for network: ${contract.chain}"
        )
        log.info("Starting trace_filter data ingestion for contract $contract")


        log.info("Starting ingestion from block $startBlock to target latest block $endBlock")

        var lastProcessedBlock = startBlock
        var completed = false

        // Process blocks in batches until we reach the target latest block
        while (!completed) {
            try {
                // Check if we've reached the target block
                if (lastProcessedBlock >= endBlock) {
                    log.info("Reached target latest block $endBlock. Ingestion complete.")
                    completed = true
                } else {
                    // Calculate the next batch to process
                    val toBlock = minOf(lastProcessedBlock + config.batchSize, endBlock)
                    log.info("Processing blocks ${lastProcessedBlock + 1} to $toBlock with trace_filter (${toBlock - lastProcessedBlock} blocks)")

                    // Process the block range using trace_filter
                    processBlockRangeWithTraceFilter(
                        contract.chain, lastProcessedBlock + 1, toBlock, contract.address.lowercase(Locale.getDefault())
                    )

                    lastProcessedBlock = toBlock

                    // Calculate and log progress
                    val progressPercentage =
                        ((lastProcessedBlock - startBlock).toDouble() / (endBlock - startBlock).toDouble() * 100).toInt()
                    log.info("Progress: $progressPercentage% (processed up to block $lastProcessedBlock of $endBlock)")
                }
            } catch (e: Exception) {
                log.error("Error during trace_filter ingestion: ${e.message}", e)
                delay(config.pollingInterval)
            }
        }

        log.info("Ingestion run completed successfully. Processed blocks $startBlock to $endBlock")
    }

    private suspend fun processBlockRangeWithTraceFilter(
        network: String,
        fromBlock: Long,
        toBlock: Long,
        toAddress: String
    ) {
        val traces = getTraces(fromBlock, toBlock, toAddress, network)
        if (traces.isEmpty()) return

        val invocations = traces.parMap(concurrency = 8) { t ->
            try {
                /* ---------- identity ------------ */
                val txHash      = t["transactionHash"]?.asText() ?: return@parMap null
                val traceArray  = t["traceAddress"]
                val tracePath   = if (traceArray == null || traceArray.isEmpty)
                    "" else traceArray.joinToString(".") { it.asInt().toString() }
                val depth       = if (tracePath.isEmpty()) 0 else tracePath.count { it == '.' } + 1

                val blockNumHex = t["blockNumber"]?.asText() ?: return@parMap null
                val blockNumber = Numeric.decodeQuantity(blockNumHex).longValueExact()

                /* ---------- call meta ----------- */
                val action      = t["action"]
                val result      = t["result"]
                val callType    = action?.get("callType")?.asText()
                val traceType   = t["type"]?.asText() ?: "call"

                /* ---------- participants -------- */
                val from        = action?.get("from")?.asText() ?: return@parMap null
                val to          = action?.get("to")?.asText()
                val valueWei    = Numeric.decodeQuantity(action?.get("value")?.asText() ?: "0x0")

                /* ---------- gas ----------------- */
                val gasSupplied = Numeric.decodeQuantity(action?.get("gas")?.asText() ?: "0x0")
                val gasUsed     = Numeric.decodeQuantity(result?.get("gasUsed")?.asText() ?: "0x0")

                /* ---------- payload ------------- */
                val input       = action?.get("input")?.asText()
                val output      = result?.get("output")?.asText()
                val functionSel = input?.takeIf { it.length >= 10 }?.substring(0, 10)

                /* ---------- timestamp ----------- */
                val block       = blocks.getBlockByNumber(BigInteger(blockNumHex), network)

                RawInvocationData(
                    network          = network,
                    contractAddress  = toAddress,
                    blockNumber      = blockNumber,
                    blockTimestamp   = Instant.ofEpochSecond(block.timestamp.longValueExact()),
                    txHash           = txHash,

                    tracePath        = tracePath,
                    depth            = depth,
                    callType         = callType,
                    traceType        = traceType,

                    from             = from,
                    to               = to,
                    valueWei         = valueWei,

                    gas              = gasSupplied,
                    gasUsed          = gasUsed,

                    functionSelector = functionSel,
                    input            = input,
                    output           = output
                )
            } catch (e: Exception) {
                log.error("Trace parse error: ${e.message}", e)
                null
            }
        }.filterNotNull()          // let the DB handle true duplicates

        if (invocations.isNotEmpty()) {
            rawInvocationsRepository.batchInsert(invocations)
            log.info("Saved ${invocations.size} raw invocations")
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