package org.cipheredge.rest.request

import java.math.BigInteger

class GetEventLogsRequest(
    val addresses: List<String>,
    val topic: String,
    val optionalTopics: List<String?> = emptyList(),
    val fromBlock: BigInteger? = null,
    val toBlock: BigInteger? = null,
    val fetchAll: Boolean? = true
)