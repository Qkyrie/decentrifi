package com.decentri.balance.rest.vo

data class TokenBalancesVO(
    val balances: List<BalanceElementVO>
) {

    val totalDollarValue: Double = balances.sumOf { it.dollarValue }
}

fun noBalance(): TokenBalancesVO {
    return TokenBalancesVO(emptyList())
}