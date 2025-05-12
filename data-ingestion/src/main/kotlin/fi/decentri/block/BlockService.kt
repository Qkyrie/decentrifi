package fi.decentri.block

import fi.decentri.application.ports.BlockPort
import fi.decentri.dataingest.config.Web3jManager
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthBlock
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.minutes

class BlockService(private val web3jManager: Web3jManager) : BlockPort {

    /**
     * Returns the block number closest to the specified time.
     * Uses the current time and block, then calculates the estimated block number
     * based on the average block time for the network.
     *
     * @param targetTime The target time to find a block for
     * @param network The blockchain network (defaults to "ethereum")
     * @return The estimated block number closest to the target time
     */
    override suspend fun getBlockClosestTo(targetTime: LocalDateTime, network: String): Long {
        val web3 = web3jManager.web3(network) ?: throw IllegalArgumentException("Network '$network' is not configured")

        // Get the current block (latest)
        val latestBlock = web3.web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
            .send().block

        // Convert current block timestamp to LocalDateTime
        val currentBlockTime = Instant.ofEpochSecond(latestBlock.timestamp.longValueExact())
            .atZone(ZoneOffset.UTC)
            .toLocalDateTime()

        // Get the current block number
        val currentBlockNumber = latestBlock.number.longValueExact()

        // Calculate time difference between target time and current block time
        val timeDifference = Duration.between(targetTime, currentBlockTime)

        // Get the average block time for the network
        val networkConfig = web3jManager.getNetworkConfig(network)
        val avgBlockTime = networkConfig?.blockTime ?: 12 // seconds

        // Calculate estimated number of blocks between the times
        val blockDifference = timeDifference.seconds / avgBlockTime

        // Calculate the target block number
        val targetBlockNumber = currentBlockNumber - blockDifference

        // Ensure we don't return a negative block number
        return maxOf(0, minOf(currentBlockNumber, targetBlockNumber))
    }

    /**
     * Gets the latest block number for the specified network.
     *
     * @param network The blockchain network (defaults to "ethereum")
     * @return The latest block number
     */
    override suspend fun getLatestBlock(network: String): Long {
        val web3j = web3jManager.web3(network) ?: throw IllegalArgumentException("Network '$network' is not configured")
        return web3j.web3j.ethBlockNumber().send().blockNumber.longValueExact()
    }

    // Cache block data to avoid redundant calls
    private val blockCache = Cache.Builder<Pair<String, BigInteger>, EthBlock.Block>()
        .expireAfterWrite(5.minutes)
        .build()

    /**
     * Gets a block by its number from the specified network.
     *
     * @param number The block number
     * @param network The blockchain network (defaults to "ethereum")
     * @return The block data
     */
    override suspend fun getBlockByNumber(number: BigInteger, network: String): EthBlock.Block {
        val web3j = web3jManager.web3(network) ?: throw IllegalArgumentException("Network '$network' is not configured")

        return withContext(Dispatchers.IO) {
            blockCache.get(Pair(network, number)) {
                web3j.web3j.ethGetBlockByNumber(
                    DefaultBlockParameter.valueOf(number), false
                ).sendAsync().await().block
            }
        }
    }
}