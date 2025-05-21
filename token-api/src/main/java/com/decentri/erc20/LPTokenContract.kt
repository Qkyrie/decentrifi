package com.decentri.erc20

import kotlinx.coroutines.Deferred
import java.math.BigDecimal
import java.math.BigInteger
import fi.decentri.evm.ERC20Contract
import fi.decentri.evm.FormatUtilsExtensions.asEth
import fi.decentri.evm.TypeUtils
import fi.decentri.evm.TypeUtils.Companion.uint256

class LPTokenContract(address: String) : ERC20Contract(address) {

    val token0: Deferred<String> = constant("token0", TypeUtils.address())
    val token1: Deferred<String> = constant("token1", TypeUtils.address())

    val totalSupply: Deferred<BigInteger> = constant("totalSupply", uint256())
    val decimals: Deferred<BigInteger> = constant("decimals", uint256())

    suspend fun totalDecimalSupply(): BigDecimal {
        return totalSupply.await().asEth(decimals.await())
    }
}