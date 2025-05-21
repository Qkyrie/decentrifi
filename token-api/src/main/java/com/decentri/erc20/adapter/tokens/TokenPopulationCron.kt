package com.decentri.erc20.adapter.tokens

import com.decentri.erc20.application.TokenInformationService
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

@Configuration
@ConditionalOnProperty("token-population.enabled", havingValue = "true")
@ConditionalOnBean(ExternalTokenExternalListResource::class)
class TokenPopulationCron(
    private val resource: ExternalTokenExternalListResource,
    private val tokenInformationService: TokenInformationService,
) {

    val logger = LoggerFactory.getLogger(this::class.java)


    @PostConstruct
    fun run() {
        Executors.newSingleThreadExecutor().submit {
            runBlocking {
                logger.info("Starting token population")
                resource.populateTokens()
                tokenInformationService.initialPopulation()
                logger.info("end of token population")
            }
        }
    }
}