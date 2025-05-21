package com.decentri.erc20.application.repository

import fi.decentri.evm.Chain

data class ExternalToken(
    val address: String,
    val symbol: String,
    val name: String,
    val network: Chain,
    val decimals: Int,
    val logoURI: String,
)