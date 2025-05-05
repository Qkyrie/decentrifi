package fi.decentri.block

import io.github.reactivecircus.cache4k.Cache
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthBlock
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.minutes

class BlockService(private val web3j: Web3j) {

    val blockTimes = mapOf(
        "ethereum" to 12,
    )

    /**
     * Returns the block number closest to the specified time.
     * Uses the current time and block, then calculates the estimated block number
     * based on the average block time for the network.
     *
     * @param targetTime The target time to find a block for
     * @param network The blockchain network (defaults to "ethereum")
     * @return The estimated block number closest to the target time
     */
    fun getBlockClosestTo(targetTime: LocalDateTime, network: String = "ethereum"): Long {
        // Get the current block (latest)
        val latestBlock = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
            .send().block

        // Convert current block timestamp to LocalDateTime
        val currentBlockTime = Instant.ofEpochSecond(latestBlock.timestamp.longValueExact())
            .atZone(ZoneOffset.UTC)
            .toLocalDateTime()

        // Get the current block number
        val currentBlockNumber = latestBlock.number.longValueExact()

        // Calculate time difference between target time and current block time
        val timeDifference = Duration.between(targetTime, currentBlockTime)

        // Get the average block time for the network (defaults to 12s for ethereum)
        val avgBlockTime = blockTimes[network] ?: 12 // seconds

        // Calculate estimated number of blocks between the times
        val blockDifference = timeDifference.seconds / avgBlockTime

        // Calculate the target block number
        val targetBlockNumber = currentBlockNumber - blockDifference

        // Ensure we don't return a negative block number
        return minOf(currentBlockNumber, targetBlockNumber)
    }

    fun getLatestBlock(): Long {
        return web3j.ethBlockNumber().send().blockNumber.longValueExact()
    }

    val blockCache = Cache.Builder<BigInteger, EthBlock.Block>()
        .expireAfterWrite(5.minutes)
        .build()

    suspend fun getBlockByNumber(number: BigInteger): EthBlock.Block {
        return blockCache.get(number) {
            web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(number), false
            ).send().block
        }
    }
}