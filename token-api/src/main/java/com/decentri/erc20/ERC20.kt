package com.decentri.erc20

import fi.decentri.evm.Chain
import java.math.BigInteger

class ERC20(
    val name: String,
    val symbol: String,
    val decimals: Int,
    val network: Chain,
    val address: String,
    val totalSupply: BigInteger,
)