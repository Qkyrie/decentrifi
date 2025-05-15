package org.cipheredge.rest

import arrow.core.Either
import arrow.core.getOrElse
import org.cipheredge.chain.Chain
import org.cipheredge.chain.evm.config.Web3jHolder
import org.cipheredge.chain.evm.web3j.Web3JProxyImpl
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger

@RestController
@RequestMapping("/{chain}/balance")
class NativeBalanceRestController(
    private val web3JProxyImpl: Web3JProxyImpl,
    private val web3jHolder: Web3jHolder,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/{address}")
    suspend fun getNativeBalance(
        @PathVariable("chain") chain: String,
        @PathVariable("address") address: String
    ): BigInteger {
        val n = Chain.fromString(chain)
        return when {
            n.addressValidator.isValid(address).not() -> BigInteger.ZERO
            n.isEVM -> Either.catch {
                web3JProxyImpl.ethGetBalance(
                    address,
                    web3jHolder.getWeb3j(n)
                ).balance
            }.mapLeft {
                logger.error("Failed to get balance for $address on $n: ${it.message}")
            }.getOrElse { BigInteger.ZERO }

            else -> BigInteger.ZERO
        }
    }
}