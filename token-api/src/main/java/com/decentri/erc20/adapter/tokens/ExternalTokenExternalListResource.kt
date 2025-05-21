package com.decentri.erc20.adapter.tokens

import arrow.core.Either
import arrow.core.getOrElse
import com.decentri.erc20.application.repository.ExternalToken
import com.decentri.erc20.application.repository.ExternalTokenListResource
import com.decentri.erc20.application.repository.TokenListResponse
import com.fasterxml.jackson.databind.ObjectMapper
import fi.decentri.evm.Chain
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.web3j.protocol.Network

@Component
class ExternalTokenExternalListResource(
    private val objectMapper: ObjectMapper,
    private val client: HttpClient
) : ExternalTokenListResource {

    val baseUrl = "https://raw.githubusercontent.com/Qkyrie/tokenlists/main/"

    val logger: Logger = LoggerFactory.getLogger(ExternalTokenListResource::class.java)

    private var tokenList: Map<Chain, List<ExternalToken>> = emptyMap()

    suspend fun populateTokens() {
        tokenList = listOf(
            "$baseUrl/ethereum/uniswap-default.tokenlist.json",
            "$baseUrl/polygon/quickswap-default.tokenlist.json",
            "$baseUrl/polygon/polygon.vetted.tokenlist.json",
            "$baseUrl/polygon/polygon.listed.tokenlist.json",
            "$baseUrl/ethereum/extendedtokens.uniswap.json",
            "$baseUrl/arbitrum/tokenlist.json",
            "$baseUrl/arbitrum/camelot-tokens.json",
            "$baseUrl/optimism/optimism.tokenlist.json",
            "$baseUrl/polygon-zkevm/tokenlist.json",
            "$baseUrl/base/tokenlist.json",
            "$baseUrl/ink/relay.ink.tokenlist.json"
        ).flatMap {
            try {
                fetchFromTokenList(it)
            } catch (exception: Exception) {
                logger.error("failed to fetch $it", exception)
                emptyList()
            }
        }.groupBy {
            it.network
        }
    }

    private suspend fun fetchFromTokenList(url: String): List<ExternalToken> {
        val result: String = withContext(Dispatchers.IO) {
            client.get(with(HttpRequestBuilder()) {
                url(url)
                this
            }).bodyAsText()
        }

        val tokens = objectMapper.readValue(
            result,
            TokenListResponse::class.java
        )

        logger.info("imported $url")

        return tokens.tokens.filter {
            it.chainId in Chain.entries.map { it.chainId } //only supported networks
        }.filter {
            it.decimals != 0
        }.flatMap { entry ->
            Either.catch {
                entry.accumulate()
            }.mapLeft {
                logger.error("failed to accumulate token", it)
            }.getOrElse { emptyList() }
        }
    }

    override fun allTokens(network: Chain): List<ExternalToken> {
        val tokens = tokenList[network] ?: emptyList()
        return tokens.distinctBy {
            it.address.lowercase()
        }
    }
}


val NATIVE_WRAP_MAPPING = mapOf(
    Chain.ETHEREUM to "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2",
)