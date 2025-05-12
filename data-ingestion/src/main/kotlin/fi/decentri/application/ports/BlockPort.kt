package fi.decentri.application.ports

import org.web3j.protocol.core.methods.response.EthBlock
import java.math.BigInteger
import java.time.LocalDateTime

interface BlockPort {
    suspend fun getBlockClosestTo(targetTime: LocalDateTime, network: String): Long
    suspend fun getLatestBlock(network: String): Long
    suspend fun getBlockByNumber(number: BigInteger, network: String): EthBlock.Block
}