package org.cipheredge.chain.evm

import org.cipheredge.rest.request.EvmContractInteractionRequest
import org.cipheredge.rest.request.EvmReadContractStateRequest
import org.springframework.stereotype.Component
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type

@Component
class ReadStateToNativeCallConverter {

    fun convert(readContractStateRequest: EvmReadContractStateRequest): EvmContractInteractionRequest {
        val function = createFunction(
            readContractStateRequest.method,
            readContractStateRequest.inputs.map { it.typed() },
            readContractStateRequest.outputs.map { it.makeTypeReference() }
        )
        val encodedFunction = FunctionEncoder.encode(function)
        return EvmContractInteractionRequest(
            contract = readContractStateRequest.contract,
            function = encodedFunction
        )
    }

    fun createFunction(
        method: String,
        inputs: List<Type<*>> = emptyList(),
        outputs: List<TypeReference<out Type<*>>>? = emptyList()
    ): Function {
        return Function(
            method,
            inputs,
            outputs
        )
    }


}