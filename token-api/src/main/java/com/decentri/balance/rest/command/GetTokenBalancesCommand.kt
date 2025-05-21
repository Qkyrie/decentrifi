package com.decentri.balance.rest.command

data class GetTokenBalancesCommand(val token: String,
                                   val network: String,
                                   val addresses: List<String>)