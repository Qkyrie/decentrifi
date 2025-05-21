package com.decentri.token.rest.vo

data class TokenHolder(
    val user: String,
    val tokens: List<Token> = emptyList()
)

data class Token(
    val address: String,
    val network: String,
    val holds: Boolean
)