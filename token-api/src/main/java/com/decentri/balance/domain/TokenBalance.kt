package com.decentri.balance.domain

import java.math.BigInteger

data class TokenBalance(
    val token: String,
    val address: String,
    val balance: BigInteger
)