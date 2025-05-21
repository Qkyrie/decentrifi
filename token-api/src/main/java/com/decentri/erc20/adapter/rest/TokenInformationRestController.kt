package com.decentri.erc20.adapter.rest

import arrow.core.getOrElse
import com.decentri.erc20.FungibleTokenVO
import com.decentri.erc20.adapter.rest.vo.WrappedTokenVO
import com.decentri.erc20.adapter.tokens.NATIVE_WRAP_MAPPING
import com.decentri.erc20.domain.toVO
import com.decentri.erc20.port.input.TokenInformationUseCase
import fi.decentri.evm.Chain
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class TokenInformationRestController(
    private val tokenInformationUseCase: TokenInformationUseCase
) {

    val logger = LoggerFactory.getLogger(this::class.java)


    @GetMapping("/{network}")
    suspend fun getAllTokensForNetwork(
        @PathVariable("network") networkName: String,
        @RequestParam("verified", defaultValue = "false") verified: Boolean
    ): ResponseEntity<List<FungibleTokenVO>> {
        val network = Chain.fromStringOrNull(networkName) ?: return ResponseEntity.badRequest().build()
        try {
            return ResponseEntity.ok(
                tokenInformationUseCase.getAllSingleTokens(network, verified)
                    .map {
                        it.toVO()
                    }
            )
        } catch (ex: Exception) {
            logger.debug("Error while getting token information", ex)
            return ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/{network}/wrapped")
    fun getWrappedToken(@PathVariable("network") networkName: String): ResponseEntity<WrappedTokenVO> {
        val network = Chain.fromStringOrNull(networkName) ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(
            WrappedTokenVO(
                NATIVE_WRAP_MAPPING[network]!!
            )
        )
    }

    @GetMapping("/{network}/{address}/token")
    suspend fun getTokenInformation(
        @PathVariable("network") networkName: String,
        @PathVariable("address") address: String
    ): ResponseEntity<FungibleTokenVO> = coroutineScope {
        val network = Chain.fromStringOrNull(networkName) ?: throw IllegalArgumentException("Invalid network")
        try {
            tokenInformationUseCase.getTokenInformation(address, network).map {
                ResponseEntity.ok(it.toVO())
            }.getOrElse {
                ResponseEntity.notFound().build()
            }
        } catch (ex: Exception) {
            logger.error(
                "Error while getting token information for token: {} and network: {}. {}",
                address,
                network,
                ex.message
            )
            ResponseEntity.notFound().build()
        }
    }
}