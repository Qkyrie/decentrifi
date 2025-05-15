package fi.decentri.evm.request

import java.math.BigInteger

data class GetTransactionHistoryRequest(
    val user: String,
    val contract: String?,
    val methodId: String? = null,
    val startBlock: BigInteger? = null,
    val includeData: Boolean,
)
