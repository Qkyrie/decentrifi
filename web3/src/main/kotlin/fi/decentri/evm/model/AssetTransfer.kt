package fi.decentri.evm.model

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime

data class AssetTransfer(
    val uniqueId: String,
    val asset: String,
    val amount: BigDecimal,
    val from: String,
    val to: String,
    val contractAddress: String,
    val timestamp: LocalDateTime,
    val hash: String,
    val network: String,
    val blockNumber: BigInteger,
    val erc721TokenId: String? = null,
)