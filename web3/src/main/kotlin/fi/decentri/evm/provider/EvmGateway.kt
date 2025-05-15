package fi.decentri.evm.provider

import arrow.core.Either
import arrow.core.getOrElse
import fi.decentri.evm.EvmContract
import fi.decentri.evm.model.ContractCall
import fi.decentri.evm.model.MultiCallResult
import com.github.michaelbull.retry.policy.binaryExponentialBackoff
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import com.google.gson.JsonParser
import fi.decentri.evm.MultiCallCaller
import fi.decentri.evm.command.EvmContractInteractionCommand
import fi.decentri.evm.command.GetEventLogsCommand
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Type
import org.web3j.protocol.core.methods.response.EthCall
import org.web3j.protocol.core.methods.response.EthLog
import java.util.Collections.emptyList
import org.web3j.abi.datatypes.Function as Web3Function


class EvmGateway(
    network: String,
    private val multicallCaller: MultiCallCaller,
    httpClient: HttpClient,
    endpoint: String,
) : ChainLayer(
    httpClient,
    endpoint,
    network
), MultiCallCaller by multicallCaller {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun readMultiCall(
        elements: List<ContractCall>,
    ): List<MultiCallResult> {
        return Either.catch {
            multicallCaller.readMultiCall(elements) { address, function ->
                executeCall(address, function)
            }
        }.mapLeft {
            logger.error(
                """Error reading multicall for network $network. 
                exception was ${it.message}
                There were ${elements.size} elements. addresses: ${
                    elements.joinToString(
                        ","
                    ) { it.address }
                }""", it)
            emptyList<MultiCallResult>()
        }.getOrElse { emptyList() }
    }


    suspend fun readFunction(
        address: String,
        function: String,
        inputs: List<Type<*>>,
        outputs: List<TypeReference<out Type<*>>>? = null
    ): List<Type<*>> {
        logger.debug("reading $function from $address")
        return executeCall(address, createFunction(function, inputs, outputs))
    }

    suspend fun executeCall(
        address: String,
        function: org.web3j.abi.datatypes.Function,
    ): List<Type<*>> {
        val encodedFunction = FunctionEncoder.encode(function)
        val ethCall = call(null, address, encodedFunction) ?: return emptyList()
        return FunctionReturnDecoder.decode(ethCall.value, function.outputParameters)
    }

    suspend fun call(
        from: String?,
        contract: String,
        encodedFunction: String
    ): EthCall? {
        return retry(limitAttempts(5) + binaryExponentialBackoff(200, 3000)) {
            val post = post("/contract/call") {
                contentType(ContentType.Application.Json)
                setBody(
                    EvmContractInteractionCommand(
                        from = from,
                        contract = contract,
                        function = encodedFunction
                    )
                )
            }

            if (!post.status.isSuccess()) {
                null
            } else {
                post.body()
            }
        }
    }


    companion object {
        fun createFunction(
            method: String,
            inputs: List<Type<*>> = kotlin.collections.emptyList(),
            outputs: List<TypeReference<out Type<*>>>? = kotlin.collections.emptyList()
        ): org.web3j.abi.datatypes.Function {
            return Web3Function(
                method,
                inputs,
                outputs
            )
        }
    }

    suspend fun getEventsAsEthLog(getEventsLog: GetEventLogsCommand): List<EthLog.LogObject> {
        val result = post("/events/logs") {
            contentType(ContentType.Application.Json)
            setBody(getEventsLog)
        }
        return if (result.status.isSuccess()) {
            val body: String = result.body()
            val parsed = JsonParser.parseString(body).asJsonObject
            if (parsed.has("error") && !parsed["error"].isJsonNull) {
                logger.error("Unable to get events from blockchain, result was ${result.bodyAsText()}")
                return kotlin.collections.emptyList()
            }
            parsed["result"].asJsonArray.map {
                mapper.readValue(it.toString(), EthLog.LogObject::class.java)
            }
        } else {
            logger.error("Unable to get events from blockchain, result was ${result.bodyAsText()}")
            return emptyList()
        }
    }

    suspend fun <T : EvmContract> contractAt(creator: suspend () -> T): T {
        return creator().apply {
            gateway = this@EvmGateway
        }
    }
}
