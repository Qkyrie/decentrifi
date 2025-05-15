package fi.decentri.evm.request

import java.math.BigInteger

data class FindAssetTransfersRequest(
    val tokens: List<String> = emptyList(),
    val to: String?,
    val from: String?,
    val allPages: Boolean = true,
    val excludeZeroValue: Boolean = false,
    val fromBlock: BigInteger? = null
)