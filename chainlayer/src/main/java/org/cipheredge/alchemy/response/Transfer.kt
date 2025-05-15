package org.cipheredge.alchemy.response

import java.math.BigDecimal
import java.math.BigInteger

data class Transfer(
    val uniqueId: String,
    val asset: String?,
    val from: String,
    val to: String?,
    val value: BigDecimal?,
    val rawContract: RawContract,
    val hash: String,
    val metadata: Metadata,
    val blockNum: String,
    val category: String,
    val erc721TokenId: String?,
) {
    val blockNumber = BigInteger(blockNum.removePrefix("0x"), 16)
}
