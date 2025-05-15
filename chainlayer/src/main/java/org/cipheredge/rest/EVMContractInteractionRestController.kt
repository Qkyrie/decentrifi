package org.cipheredge.rest

import arrow.core.Either
import arrow.core.getOrElse
import org.cipheredge.chain.EthCallResultToTypeConverter
import org.cipheredge.chain.Chain
import org.cipheredge.chain.evm.ReadStateToNativeCallConverter
import org.cipheredge.chain.evm.config.Web3jHolder
import org.cipheredge.chain.evm.web3j.Web3JProxyImpl
import org.cipheredge.rest.request.EvmContractInteractionRequest
import org.cipheredge.rest.request.EvmReadContractStateRequest
import org.cipheredge.rest.request.output.Output
import org.cipheredge.web3j.TypeUtils
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.web3j.abi.EventEncoder
import org.web3j.abi.TypeDecoder
import org.web3j.abi.datatypes.Event
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthCall
import java.math.BigInteger

@RestController
@RequestMapping("/{chain}/contract")
class EVMContractInteractionRestController(
    private val web3JProxyImpl: Web3JProxyImpl,
    private val web3jHolder: Web3jHolder,
    private val readStateToNativeCallConverter: ReadStateToNativeCallConverter,
    private val ethCallResultToTypeConverter: EthCallResultToTypeConverter,
) {


    @PostMapping("/call")
    suspend fun postCall(
        @PathVariable("chain") chain: String,
        @RequestBody evmContractInteractionRequest: EvmContractInteractionRequest
    ): EthCall {
        val n = Chain.fromString(chain)
        return when {
            n.isEVM -> call(evmContractInteractionRequest, n)
            else -> {
                throw IllegalArgumentException("Unsupported chain: $chain")
            }
        }
    }

    @GetMapping("/{contract}/find-proxy")
    suspend fun getProxy(
        @PathVariable("contract") address: String,
        @PathVariable("chain") chain: String
    ): ResponseEntity<Proxy> {
        val n = Chain.fromString(chain)
        return Either.catch {
            val result: org.web3j.abi.datatypes.Address = TypeDecoder.decode(
                web3jHolder.getWeb3j(n).ethGetStorageAt(
                    address,
                    BigInteger("360894a13ba1a3210667c828492db98dca3e2076cc3735a920a3ca505d382bbc", 16),
                    DefaultBlockParameterName.LATEST
                )?.send()?.result, TypeUtils.address()
            )
            result.value
        }.map {
            ResponseEntity.ok(Proxy(it))
        }.getOrElse { ResponseEntity.notFound().build() }
    }

    data class Proxy(val implementation: String)

    data class BuildEventTopicEvent(
        val name: String,
        val arguments: List<Output>
    )

    @PostMapping("/build-event-topic")
    fun buildEventTopic(
        @RequestBody buildEventTopicEvent: BuildEventTopicEvent
    ): String? {
        val eventName = buildEventTopicEvent.name.takeWhile { it != '(' }
        val event = Event(
            eventName,
            buildEventTopicEvent.arguments.map {
                it.makeTypeReference()
            }
        )
        return EventEncoder.encode(event) ?: "no topic created"
    }

    @PostMapping("/read")
    suspend fun readState(
        @PathVariable("chain") chain: String,
        @RequestBody evmReadContractStateRequest: EvmReadContractStateRequest
    ): List<EthCallResultToTypeConverter.Result> {
        val n = Chain.fromString(chain)
        return when {
            n.isEVM -> {
                val outputs = evmReadContractStateRequest.outputs.map { it.makeTypeReference() }
                val called = call(readStateToNativeCallConverter.convert(evmReadContractStateRequest), n)
                ethCallResultToTypeConverter.convert(outputs, called.value)
            }

            else -> {
                throw IllegalArgumentException("Unsupported chain: $chain")
            }
        }
    }

    private suspend fun call(
        evmContractInteractionRequest: EvmContractInteractionRequest,
        chain: Chain
    ): EthCall {
        return try {
            web3JProxyImpl.call(evmContractInteractionRequest, web3jHolder.getWeb3j(chain))
        } catch (e: Exception) {
            throw IllegalArgumentException("Error calling contract: ${e.message}", e)
        }
    }
}