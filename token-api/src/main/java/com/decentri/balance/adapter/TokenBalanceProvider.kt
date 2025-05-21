package com.decentri.balance.adapter

import arrow.core.Either
import arrow.core.getOrElse
import com.decentri.balance.domain.TokenBalance
import com.decentri.balance.port.TokenBalances
import com.decentri.balance.rest.exception.InvalidAddressException
import com.decentri.erc20.application.TokenInformationService
import com.decentri.erc20.domain.toVO
import fi.decentri.evm.Chain
import fi.decentri.evm.ERC20Contract
import fi.decentri.evm.TypeUtils.Companion.toAddress
import fi.decentri.evm.provider.ChainLayerProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class TokenBalanceProvider(
    private val erC20TokenInformationService: TokenInformationService,
) : TokenBalances {

    val chainlayerProvider = ChainLayerProvider.getInstance()

    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun getNativeBalance(address: String, network: Chain): BigInteger {
        if (network.addressValidator.isValid(address).not()) {
            logger.info("Invalid address $address for network $network. Returning 0 balance.")
            return BigInteger.ZERO
        }

        return Either.catch {
            chainlayerProvider.getChainLayer(network).getNativeBalance(address)
        }.getOrElse {
            logger.error(it.message)
            BigInteger.ZERO
        }
    }

    override suspend fun getTokenBalances(
        tokenAddress: String,
        network: Chain,
        addresses: List<String>
    ): List<TokenBalanceVO> {
        return getEvmTokenBalance(addresses, network, tokenAddress).mapNotNull {
            val token = erC20TokenInformationService.getTokenInformation(it.token, network).getOrNull()
                ?: return@mapNotNull null
            TokenBalanceVO(
                amount = it.balance,
                token = token.toVO(),
                network = network,
                wallet = it.address
            )
        }
    }

    private suspend fun getEvmTokenBalance(
        addresses: List<String>,
        network: Chain,
        token: String
    ): List<TokenBalance> {
        validateAddresses(addresses)

        val gateway = chainlayerProvider.getEvmChainLayer(network)

        val erc20 = gateway.contractAt { ERC20Contract(token) }

        val multiResults = gateway.readMultiCall(
            addresses.filter {
                it != "0x0" && it != "0x" //no native tokens
            }.map { erc20.balanceOfFunction(it) }
        )

        return multiResults.zip(addresses).map { (result, address) ->
            if (result.success) {
                TokenBalance(token, address, result.data[0].value as BigInteger)
            } else {
                TokenBalance(token, address, BigInteger.ZERO)
            }
        }
    }

    override suspend fun getTokenBalances(
        network: Chain,
        user: String
    ): List<TokenBalanceVO> {
        return try {

            if (network.addressValidator.isValid(user).not()) {
                logger.info("Invalid address $user for network $network. Returning no balances.")
                return emptyList()
            }

            getEvmTokenBalances(user, network)
        } catch (ex: Exception) {
            logger.error("Unable to get token balances for {}", network)
            emptyList()
        }
    }

    private suspend fun getEvmTokenBalances(user: String, network: Chain): List<TokenBalanceVO> {
        val tokenAddresses = erC20TokenInformationService.getAllSingleTokens(network, true).map {
            it.address
        }.filter {
            it != "0x0" && it != "0x" //no native tokens
        }

        return getBalancesFor(user, tokenAddresses, network)
            .mapIndexed { i, balance ->
                if (balance > BigInteger.ZERO) {
                    val token =
                        erC20TokenInformationService.getTokenInformation(tokenAddresses[i], network).getOrElse {
                            return@mapIndexed null
                        }
                    TokenBalanceVO(
                        amount = balance,
                        token = token.toVO(),
                        network = network,
                        wallet = user
                    )
                } else {
                    null
                }
            }.filterNotNull()
    }


    private fun validateAddresses(addresses: List<String>) {
        val failures = addresses.map {
            it to Either.catch {
                it.toAddress()
            }
        }.filter { (_, validation) ->
            validation.isLeft()
        }

        if (failures.isNotEmpty()) {
            throw InvalidAddressException("Invalid addresses: ${failures.joinToString(",") { it.first }}")
        }
    }

    private suspend fun getBalancesFor(
        address: String,
        tokens: List<String>,
        network: Chain,
    ): List<BigInteger> {
        val chainlayerProvider = ChainLayerProvider.getInstance()
        return with(chainlayerProvider.getEvmChainLayer(network)) {
            readMultiCall(
                tokens.map { contractAt { ERC20Contract(it) } }
                    .map { token ->
                        token.balanceOfFunction(address)
                    }).map {
                try {
                    if (!it.success) {
                        BigInteger.ZERO
                    } else {
                        it.data[0].value as BigInteger
                    }
                } catch (_: Exception) {
                    BigInteger.ZERO
                }
            }
        }
    }
}