package com.decentri.erc20.domain

import com.decentri.erc20.FungibleTokenVO
import fi.decentri.evm.Chain
import java.math.BigInteger

data class FungibleToken(
    val network: Chain,
    val logo: String? = null,
    val name: String,
    val symbol: String,
    val address: String,
    val decimals: Int,
    val totalSupply: BigInteger = BigInteger.ZERO,
    val underlyingTokens: List<FungibleToken> = emptyList(),
    val verified: Boolean = false,
)

fun FungibleToken.toVO(): FungibleTokenVO {
    return FungibleTokenVO(
        network = network,
        logo = logo,
        name = name,
        symbol = symbol,
        address = address,
        decimals = decimals,
        totalSupply = totalSupply,
        underlyingTokens = underlyingTokens.map { it.toVO() },
        verified = verified,
    )
}