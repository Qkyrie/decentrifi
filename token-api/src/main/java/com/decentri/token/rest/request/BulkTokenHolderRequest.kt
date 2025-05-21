package com.decentri.token.rest.request

data class BulkTokenHolderRequest(
    val user: String,
    val tokens: List<Token> = emptyList()
)

