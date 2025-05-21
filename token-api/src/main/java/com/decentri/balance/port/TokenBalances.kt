package com.decentri.balance.port

import com.decentri.balance.adapter.TokenBalanceVO
import fi.decentri.evm.Chain
import java.math.BigInteger

interface TokenBalances {
    suspend fun getTokenBalances(tokenAddress: String, network: Chain, addresses: List<String>): List<TokenBalanceVO>
    suspend fun getTokenBalances(
        network: Chain,
        user: String
    ): List<TokenBalanceVO>

    suspend fun getNativeBalance(address: String, network: Chain): BigInteger
}