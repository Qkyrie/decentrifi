package fi.decentri.dataingest.ingest

import fi.decentri.dataingest.config.EthereumConfig
import fi.decentri.dataingest.repository.IngestionMetadataRepository
import fi.decentri.dataingest.repository.RawInvocationData
import fi.decentri.dataingest.repository.RawInvocationsRepository
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.EthLog
import org.web3j.protocol.core.methods.response.Log
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * Service responsible for blockchain data ingestion
 */
class IngestorService(private val config: EthereumConfig) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val web3j: Web3j = Web3j.build(HttpService(config.rpcUrl))
    private val metadataRepository = IngestionMetadataRepository()
    private val rawInvocationsRepository = RawInvocationsRepository()

    // Standard ERC20 Transfer event signature
    private val transferEventSignature = "Transfer(address,address,uint256)"
    private val transferEventTopic = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"

    /**
     * Start the blockchain data ingestion process
     */
    suspend fun startIngestingData() {
        logger.info("Starting blockchain data ingestion for contract ${config.contractAddress}")

        var lastProcessedBlock = if (config.startBlock > 0) {
            config.startBlock
        } else {
            metadataRepository.getLastProcessedBlock()
        }

        logger.info("Last processed block: $lastProcessedBlock")

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
                logger.info("Processing blocks $lastProcessedBlock to $toBlock")

                // Collect and process data
                processBlockRange(lastProcessedBlock + 1, toBlock)

                // Update the last processed block
                lastProcessedBlock = toBlock
                metadataRepository.updateLastProcessedBlock(lastProcessedBlock)

            } catch (e: Exception) {
                logger.error("Error during ingestion: ${e.message}", e)
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
        ).addSingleTopic(transferEventTopic)

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
}
