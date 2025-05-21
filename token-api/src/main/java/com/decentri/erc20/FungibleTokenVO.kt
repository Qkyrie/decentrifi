package com.decentri.erc20

import fi.decentri.evm.Chain
import fi.decentri.evm.FormatUtilsExtensions.asEth
import java.math.BigDecimal
import java.math.BigInteger

class FungibleTokenVO(
    val network: Chain,
    val logo: String? = null,
    val name: String,
    val symbol: String,
    val address: String,
    val decimals: Int,
    val totalSupply: BigInteger = BigInteger.ZERO,
    val underlyingTokens: List<FungibleTokenVO> = emptyList(),
    val verified: Boolean,
) {

    fun totalDecimalSupply(): BigDecimal {
        return totalSupply.asEth(decimals)
    }
}