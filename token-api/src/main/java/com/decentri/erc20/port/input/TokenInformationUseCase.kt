package com.decentri.erc20.port.input

import arrow.core.Option
import com.decentri.erc20.domain.FungibleToken
import fi.decentri.evm.Chain

interface TokenInformationUseCase {
    suspend fun getAllSingleTokens(network: Chain, verified: Boolean): List<FungibleToken>
    suspend fun getTokenInformation(
        address: String,
        network: Chain,
        verified: Boolean = false
    ): Option<FungibleToken>
}