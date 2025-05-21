package com.decentri.balance.rest.vo

import com.decentri.erc20.FungibleTokenVO
import fi.decentri.evm.FormatUtilsExtensions.asEth
import java.math.BigInteger

data class BalanceElementVO(
    val amount: BigInteger,
    val network: String,
    val token: FungibleTokenVO,
    val price: Double
) {
    val decimalAmount = amount.asEth(token.decimals).toDouble()
    val dollarValue = price.times(decimalAmount)

}