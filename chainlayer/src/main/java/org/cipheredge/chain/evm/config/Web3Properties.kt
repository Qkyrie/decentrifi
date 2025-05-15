package org.cipheredge.chain.evm.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "fi.decentri.web3")
data class Web3Properties(
    val chains: Map<String, ChainConfig> = emptyMap()
)

data class ChainConfig(
    val endpoint: String,
    val enabled: Boolean = true
)