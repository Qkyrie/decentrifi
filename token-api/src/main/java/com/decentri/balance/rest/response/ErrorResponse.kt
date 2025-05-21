package com.decentri.balance.rest.response

data class ErrorResponse(
    val error: Boolean,
    val message: String
)

fun errorResponse(message: String) = ErrorResponse(true, message)