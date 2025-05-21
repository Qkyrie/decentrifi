@file:OptIn(ExperimentalTime::class)

package fi.decentri.application.usecases

import arrow.fx.coroutines.parMap
import fi.decentri.evm.TypeUtils.Companion.address
import fi.decentri.evm.TypeUtils.Companion.uint256
import fi.decentri.application.ports.BlockPort
import fi.decentri.dataingest.config.Web3jManager
import fi.decentri.dataingest.model.Contract
import fi.decentri.dataingest.model.MetadataType
import fi.decentri.dataingest.model.TransferEvent
import fi.decentri.infrastructure.repository.token.TransferEventRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import org.web3j.abi.EventEncoder
import org.web3j.abi.datatypes.Event
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.EthLog
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

/**
 * Service that listens for ERC20 Transfer events and processes them
 */
class TokenTransferIngestor(
    private val web3jManager: Web3jManager,
    private val transferEventRepository: TransferEventRepository,
    private val blockService: BlockPort,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    val transferEvent = Event(
        "Transfer", listOf(
            address(true), address(true), uint256()
        )
    )

    // Default token list - currently only USDC on Ethereum
    private val DEFAULT_TOKEN_CONTRACTS = mapOf(
        "ethereum" to listOf(
            "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", // USDC,
            "0xdAC17F958D2ee523a2206206994597C13D831ec7", //USDT
        )
    )

    // Token metadata cache: token address -> (symbol, decimals)
    private val tokenMetadataCache = mutableMapOf<String, Pair<String?, Int?>>()

    /**
     * Listen for transfer events for all token contracts related to the monitored contract
     */
    suspend fun ingest(
        contract: Contract,
        startBlock: Long,
        endBlock: Long,
    ) {
        logger.info("Starting token transfer event listener for chain: ${contract.chain}")

        // Only process Ethereum for now
        val tokens = DEFAULT_TOKEN_CONTRACTS[contract.chain] ?: emptyList()
        if (tokens.isEmpty()) {
            logger.info("No token contracts configured for chain: ${contract.chain}")
            return
        }

        processTokenTransfers(contract, tokens, startBlock, endBlock)
    }

    /**
     * Process transfer events for a specific token contract
     */
    private suspend fun processTokenTransfers(
        contract: Contract,
        tokens: List<String>,
        startBlock: Long,
        endblock: Long,
    ) {
        val (_, _, _, pollingInterval, _) = web3jManager.getNetworkConfig(contract.chain)
            ?: error("Unable to get network config for ${contract.chain}")


        logger.info("Starting token transfer ingestion from block $startBlock to target latest block $endblock")

        var lastProcessedBlock = startBlock
        var completed = false

        // Process blocks in batches until we reach the target latest block
        while (!completed) {
            kotlin.runCatching {
                // Check if we've reached the target block
                if (lastProcessedBlock >= endblock) {
                    logger.info("Reached target latest block $endblock. Token transfer ingestion complete.")
                    completed = true
                } else {
                    // Calculate the next batch to process
                    val toBlock = minOf(lastProcessedBlock + 5000, endblock)
                    logger.info("Processing token transfer events for blocks ${lastProcessedBlock + 1} to $toBlock (${toBlock - lastProcessedBlock} blocks)")

                    // Process the block range to get transfer events
                    processTokenTransfersInRange(
                        contract, tokens, lastProcessedBlock + 1, toBlock
                    )

                    // Update metadata if we're monitoring a specific contract
                    lastProcessedBlock = toBlock

                    // Calculate and log progress
                    val progressPercentage =
                        ((lastProcessedBlock - startBlock).toDouble() / (endblock - startBlock).toDouble() * 100).toInt()
                    logger.info("Token transfer ingestion progress: $progressPercentage% (processed up to block $lastProcessedBlock of $endblock)")
                }
            }.onFailure {
                logger.error("Error during token transfer ingestion: ${it.message}", it)
                delay(pollingInterval)
            }
        }

        logger.info("Token transfer ingestion completed for ${tokens.size} tokens. Processed blocks $startBlock to $endblock")
    }

    /**
     * Process token transfer events in a specific block range
     */
    private suspend fun processTokenTransfersInRange(
        contract: Contract, tokens: List<String>, fromBlock: Long, toBlock: Long
    ) {
        logger.info("Fetching Transfer events from block $fromBlock to $toBlock for ${tokens.size} token contracts")

        kotlin.runCatching {
            val logs = fetchTransferLogsFrom(
                contract.address, contract.chain, tokens, fromBlock, toBlock
            ) + fetchTransferLogsTo(contract.address, contract.chain, tokens, fromBlock, toBlock)

            if (logs.isEmpty()) {
                logger.debug("No Transfer events found for in blocks $fromBlock to $toBlock")
                return
            }

            logger.info("Found ${logs.size} Transfer events in blocks $fromBlock to $toBlock")

            // Get token metadata if not already cached

            // Process transfer logs in chunks to avoid memory issues
            logs.chunked(1000).forEach { chunkedLogs ->
                val timed = measureTime {
                    val transferEvents = chunkedLogs.parMap(concurrency = 16) { logObj ->
                        try {
                            processTransferLog(contract, logObj)
                        } catch (e: Exception) {
                            logger.error("Error processing Transfer log: ${e.message}", e)
                            null
                        }
                    }.filterNotNull()

                    // If we're monitoring a specific contract, filter transfers to only include those
                    // related to our monitored contract

                    if (transferEvents.isNotEmpty()) {
                        transferEventRepository.batchInsert(transferEvents)
                        logger.debug("Saved ${transferEvents.size} Transfer events to database")
                    }
                }
                logger.info("Processed ${chunkedLogs.size} Transfer logs in ${timed.inWholeSeconds} seconds")
            }
        }.onFailure {
            logger.error("Error fetching Transfer events: ${it.message}", it)
            throw it
        }
    }

    /**
     * Process a single Transfer log to extract event data
     */
    private suspend fun processTransferLog(
        contract: Contract, log: EthLog.LogObject
    ): TransferEvent? {
        // Check that this is a Transfer event (topic0 should match the signature)
        if (log.topics[0] != EventEncoder.encode(transferEvent)) {
            return null
        }

        // Extract log details
        val txHash = log.transactionHash
        val logIndex = log.logIndex.intValueExact()
        val blockNumber = log.blockNumber.longValueExact()

        // Get the block to extract timestamp
        val block = blockService.getBlockByNumber(log.blockNumber, contract.chain)
        val blockTimestamp = Instant.ofEpochSecond(block.timestamp.longValueExact())

        // Decode the from address (topic1)
        val fromAddress = if (log.topics.size > 1) {
            try {
                val fromHex = log.topics[1].replace("0x000000000000000000000000", "0x")
                fromHex
            } catch (e: Exception) {
                logger.error("Error decoding from address: ${e.message}", e)
                return null
            }
        } else {
            return null
        }

        // Decode the to address (topic2)
        val toAddress = if (log.topics.size > 2) {
            try {
                val toHex = log.topics[2].replace("0x000000000000000000000000", "0x")
                toHex
            } catch (e: Exception) {
                logger.error("Error decoding to address: ${e.message}", e)
                return null
            }
        } else {
            return null
        }

        // Decode the amount (data field for standard ERC20 Transfer)
        val amount = if (log.data != null && log.data != "0x") {
            try {
                // For standard ERC20, the amount is the only parameter in the data field
                val amountBI = BigInteger(log.data.replace("0x", ""), 16)
                BigDecimal(amountBI)
            } catch (e: Exception) {
                logger.error("Error decoding transfer amount: ${e.message}", e)
                return null
            }
        } else {
            BigDecimal.ZERO
        }

        val tokenMetadata = getTokenMetadata(contract.chain, log.address)


        // Create the transfer event
        return TransferEvent(
            network = contract.chain,
            tokenAddress = log.address,
            contractAddress = contract.address,
            txHash = txHash,
            logIndex = logIndex,
            blockNumber = blockNumber,
            blockTimestamp = blockTimestamp,
            fromAddress = fromAddress,
            toAddress = toAddress,
            amount = amount,
            tokenSymbol = tokenMetadata.first,
            tokenDecimals = tokenMetadata.second
        )
    }

    /**
     * Fetch Transfer logs for a token contract in a specific block range
     */
    private suspend fun fetchTransferLogsTo(
        to: String, chain: String, tokens: List<String>, fromBlock: Long, toBlock: Long
    ): List<EthLog.LogObject> {
        // Create filter for Transfer events
        val filter = EthFilter(
            DefaultBlockParameter.valueOf(BigInteger.valueOf(fromBlock)),
            DefaultBlockParameter.valueOf(BigInteger.valueOf(toBlock)),
            tokens
        )

        val contractPaddedAddress = "0x000000000000000000000000" + to.removePrefix("0x").lowercase()

        // Add the Transfer event signature as a filter topic
        filter.addSingleTopic(
            EventEncoder.encode(transferEvent)
        ).addNullTopic().addOptionalTopics(contractPaddedAddress)

        // Fetch logs
        val logs =
            web3jManager.web3(chain)!!.web3j.ethGetLogs(filter).sendAsync().await().logs as List<EthLog.LogObject>
        return logs
    }

    /**
     * Fetch Transfer logs for a token contract in a specific block range
     */
    private suspend fun fetchTransferLogsFrom(
        from: String, chain: String, tokens: List<String>, fromBlock: Long, toBlock: Long
    ): List<EthLog.LogObject> {
        // Create filter for Transfer events
        val filter = EthFilter(
            DefaultBlockParameter.valueOf(BigInteger.valueOf(fromBlock)),
            DefaultBlockParameter.valueOf(BigInteger.valueOf(toBlock)),
            tokens
        )

        val contractPaddedAddress = "0x000000000000000000000000" + from.removePrefix("0x").lowercase()

        // Add the Transfer event signature as a filter topic
        filter.addSingleTopic(
            EventEncoder.encode(transferEvent)
        ).addOptionalTopics(contractPaddedAddress).addNullTopic()

        // Fetch logs
        val logs =
            web3jManager.web3(chain)!!.web3j.ethGetLogs(filter).sendAsync().await().logs as List<EthLog.LogObject>
        return logs
    }

    /**
     * Get token metadata (symbol and decimals)
     */
    private suspend fun getTokenMetadata(chain: String, tokenAddress: String): Pair<String?, Int?> {
        // Check cache first
        val cacheKey = "$chain:$tokenAddress"
        tokenMetadataCache[cacheKey]?.let { return it }

        // Otherwise, query the blockchain
        var symbol: String? = null
        var decimals: Int? = null

        try {
            // Query token symbol
            val web3 = web3jManager.web3(chain)!!.web3j

            // We'll use simple calls to get the token metadata, rather than loading a full ERC20 contract
            // This avoids dependencies and is more efficient

            // Get symbol
            val symbolCall = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                "0x0000000000000000000000000000000000000000", // from address (not important)
                tokenAddress, // to address (token contract)
                "0x95d89b41" // symbol() function signature
            )
            val symbolResult = web3.ethCall(symbolCall, DefaultBlockParameter.valueOf("latest")).sendAsync().await()

            if (!symbolResult.hasError() && symbolResult.value != null && symbolResult.value != "0x") {
                symbol = try {
                    // Try to decode as a string
                    val hexValue = symbolResult.value
                    val bytes = org.web3j.utils.Numeric.hexStringToByteArray(hexValue)
                    String(bytes).trim { it <= ' ' || it.toInt() == 0 }
                } catch (e: Exception) {
                    logger.warn("Could not decode token symbol: ${e.message}")
                    null
                }
            }

            // Get decimals
            val decimalsCall = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                "0x0000000000000000000000000000000000000000",
                tokenAddress,
                "0x313ce567" // decimals() function signature
            )
            val decimalsResult = web3.ethCall(decimalsCall, DefaultBlockParameter.valueOf("latest")).sendAsync().await()

            if (!decimalsResult.hasError() && decimalsResult.value != null && decimalsResult.value != "0x") {
                decimals = try {
                    val hexValue = decimalsResult.value
                    org.web3j.utils.Numeric.toBigInt(hexValue).intValueExact()
                } catch (e: Exception) {
                    logger.warn("Could not decode token decimals: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching token metadata: ${e.message}", e)
        }

        val metadata = Pair(symbol, decimals)
        tokenMetadataCache[cacheKey] = metadata
        return metadata
    }

}