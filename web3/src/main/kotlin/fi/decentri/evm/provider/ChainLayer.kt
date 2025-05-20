package fi.decentri.evm.provider

import fi.decentri.evm.request.GetTransactionHistoryRequest
import fi.decentri.evm.response.GetTransactionHistoryResponse
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.decentri.evm.Chain
import fi.decentri.evm.model.AssetTransfer
import fi.decentri.evm.request.FindAssetTransfersRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.util.*

abstract class ChainLayer(
    val httpClient: HttpClient, val endpoint: String, val network: Chain
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    val mapper = jacksonObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false
    )

    suspend fun getNativeBalance(address: String): BigInteger {
        return try {
            val result = get("/balance/$address") {
                contentType(ContentType.Application.Json)
            }
            if (result.status.isSuccess()) {
                result.body()
            } else {
                BigInteger.ZERO
            }
        } catch (ex: Exception) {
            if (ex !is CancellationException) logger.error("Error fetching native balance for $address: ${ex.message}")

            BigInteger.ZERO
        }
    }

    suspend fun getTransactionHistory(
        user: String,
        contract: String? = null,
        methodId: String? = null,
        startBlock: BigInteger? = null,
        includeData: Boolean,
    ): List<GetTransactionHistoryResponse> {
        return try {
            val result = post("/transaction-history") {
                contentType(ContentType.Application.Json)
                setBody(
                    GetTransactionHistoryRequest(
                        user = user,
                        contract = contract,
                        methodId = methodId,
                        startBlock = startBlock,
                        includeData = includeData
                    )
                )
            }
            if (result.status.isSuccess()) {
                result.body()
            } else {
                logger.error("Unable to fetch contract interactions, status was ${result.status}")
                Collections.emptyList()
            }
        } catch (ex: Exception) {
            logger.error("Error fetching contract interactions for $user: ${ex.message}")
            Collections.emptyList()
        }
    }

    suspend fun getAssetTransfers(
        from: String?, to: String?, tokens: List<String>, allPages: Boolean, fromBlock: BigInteger? = null
    ): List<AssetTransfer> {
        return try {
            val result = post("/asset-transfers") {
                setBody(
                    FindAssetTransfersRequest(
                        from = from, to = to, tokens = tokens, allPages = allPages, fromBlock = fromBlock
                    )
                )
                contentType(ContentType.Application.Json)
            }
            if (result.status.isSuccess()) {
                result.body()
            } else {
                logger.error("Unable to fetch asset transfers, status was ${result.status}")
                logger.error("response was ${result.bodyAsText()}")
                Collections.emptyList()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            logger.error("Error fetching asset transfers for from: $from and to: $to, network: ${network}", ex)
            Collections.emptyList()
        }
    }

    suspend fun get(url: String, block: HttpRequestBuilder.() -> Unit): HttpResponse {
        return withContext(Dispatchers.IO) {
            httpClient.get("$endpoint/${url.removePrefix("/")}", block)
        }
    }

    suspend fun post(url: String, block: HttpRequestBuilder.() -> Unit): HttpResponse {
        return withContext(Dispatchers.IO) {
            httpClient.post("$endpoint/${url.removePrefix("/")}", block)
        }
    }
}