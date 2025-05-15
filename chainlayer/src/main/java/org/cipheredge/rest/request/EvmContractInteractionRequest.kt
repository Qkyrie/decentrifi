package org.cipheredge.rest.request

import java.math.BigInteger

data class EvmContractInteractionRequest(
    val from: String? = null,
    val contract: String,
    val function: String,
    val block: BigInteger? = null
)