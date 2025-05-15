package org.cipheredge.chain.evm.config

import org.cipheredge.chain.Chain
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Web3Properties::class)
class Web3jConfig(
    private val web3Properties: Web3Properties
) {
    private val logger = LoggerFactory.getLogger(Web3jConfig::class.java)

    @Bean
    fun web3jHolder(): Web3jHolder {
        val web3List = mutableListOf<Web3>()
        
        web3Properties.chains.forEach { (chainName, config) ->
            if (!config.enabled) {
                logger.info("Chain configuration for $chainName is disabled, skipping")
                return@forEach
            }
            
            try {
                val chain = Chain.fromStringOrNull(chainName)
                if (chain != null) {
                    logger.info("Adding Web3j instance for chain: ${chain.slug}")
                    web3List.add(Web3(chain, config.endpoint))
                } else {
                    logger.warn("Chain '$chainName' is configured but not found in Chain enum, skipping")
                }
            } catch (e: Exception) {
                logger.error("Failed to initialize Web3j for chain: $chainName", e)
            }
        }
        
        if (web3List.isEmpty()) {
            logger.warn("No valid Web3j instances were configured!")
        }
        
        return Web3jHolder(web3List)
    }
}