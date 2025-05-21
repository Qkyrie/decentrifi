package com.decentri.balance.adapter

import com.decentri.erc20.FungibleTokenVO
import fi.decentri.evm.Chain
import java.math.BigInteger

class TokenBalanceVO(
    val amount: BigInteger,
    val token: FungibleTokenVO,
    val network: Chain,
    val wallet: String,
)