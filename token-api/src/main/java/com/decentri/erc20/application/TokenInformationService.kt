package com.decentri.erc20.application

import arrow.core.Either.Companion.catch
import arrow.core.Option
import arrow.core.flatten
import arrow.core.some
import arrow.fx.coroutines.parMap
import com.decentri.erc20.ERC20
import com.decentri.erc20.application.repository.ExternalTokenListResource
import com.decentri.erc20.application.repository.NativeTokenRepository
import com.decentri.erc20.domain.FungibleToken
import com.decentri.erc20.port.input.TokenInformationUseCase
import com.decentri.erc20.port.output.ReadERC20Port
import fi.decentri.evm.Chain
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigInteger
import kotlin.time.measureTimedValue

@Service
class TokenInformationService(
    private val readERC20Port: ReadERC20Port,
    private val erC20Repository: ExternalTokenListResource,
    private val nativeTokenRepository: NativeTokenRepository,
    private val tokenCache: TokenCache,
    private val logoGenerator: LogoGenerator,
) : TokenInformationUseCase {

    val logger = LoggerFactory.getLogger(this.javaClass)

    override suspend fun getAllSingleTokens(network: Chain, verified: Boolean): List<FungibleToken> =
        tokenCache.find(network, verified)

    suspend fun initialPopulation() {
        Chain.entries.map { network ->
            catch {
                initialImportForNetwork(network)
            }.mapLeft {
                logger.error("Error importing tokens for network ${network.name}: {}", it.message)
            }
        }
    }

    private suspend fun initialImportForNetwork(network: Chain) {
        val timedvalue = measureTimedValue {
            val allTokens = erC20Repository.allTokens(network)
            allTokens.parMap(concurrency = 8) {
                FungibleToken(
                    network = it.network,
                    logo = it.logoURI,
                    name = it.name,
                    symbol = it.symbol,
                    address = it.address,
                    decimals = it.decimals,
                    totalSupply = BigInteger.ZERO,
                    verified = true,
                )
            }.map {
                tokenCache.put(it.address, network, it.some())
            }.filter { it.isSome() }
        }
        tokenCache.put(
            "0x0000000000000000000000000000000000000000",
            network,
            nativeTokenRepository.getNativeToken(network).some()
        )
        tokenCache.put(
            "0x0",
            network,
            nativeTokenRepository.getNativeToken(network).some()
        )

        logger.info("populating token cache for $network took ${timedvalue.duration.inWholeSeconds}s (${timedvalue.value.size}) tokens)")
    }

    override suspend fun getTokenInformation(
        address: String,
        network: Chain,
        verified: Boolean
    ): Option<FungibleToken> {
        if (address == "0x0" || address.lowercase() == "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee" || address.lowercase() == "0x0000000000000000000000000000000000000000") {
            return nativeTokenRepository.getNativeToken(network).some()
        }

        return tokenCache.get(address, network)
            ?: fetchTokenInfo(network, address, verified).also {
                tokenCache.put(address, network, it)
            }
    }

    suspend fun fetchTokenInfo(
        network: Chain,
        address: String,
        verified: Boolean = false
    ): Option<FungibleToken> = catch {

        readERC20Port.getERC20(network, address).map { token ->
            singleERC20(token)
        }.map {
            it.copy(verified = verified)
        }


    }.mapLeft {
        logger.error("Error getting token information for $address on $network: {}", it.message)
    }.getOrNone().flatten()

    private suspend fun singleERC20(token: ERC20) = FungibleToken(
        logo = logoGenerator.generate(token.network, token.address),
        name = token.name,
        symbol = token.symbol,
        address = token.address,
        decimals = token.decimals,
        totalSupply = token.totalSupply,
        network = token.network,
        verified = false,
    )
}