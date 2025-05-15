package fi.decentri.evm.command

import java.math.BigInteger

class GetEventLogsCommand(
    val addresses: List<String>,
    val topic: String,
    val optionalTopics: List<String?> = emptyList(),
    val fromBlock: BigInteger? = null,
    val toBlock: BigInteger? = null
)