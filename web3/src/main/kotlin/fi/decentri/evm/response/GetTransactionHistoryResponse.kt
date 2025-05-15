package fi.decentri.evm.response

import java.time.LocalDateTime

data class GetTransactionHistoryResponse(
    val hash: String,
    val data: String,
    val network: String,
    val timestamp: LocalDateTime
)