package org.cipheredge.chain

import arrow.core.Either
import arrow.core.getOrElse
import org.springframework.stereotype.Component
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.Utils

@Component
class EthCallResultToTypeConverter {

    fun convert(outputs: List<TypeReference<*>>, data: String): List<Result> {
        return Either.catch {
            val results = FunctionReturnDecoder.decode(
                data,
                Utils.convert(outputs)
            )
            return results.mapIndexed { index, type ->
                val output = outputs[index]
                Result(output.type.typeName, type.value)
            }
        }.getOrElse {
            if(outputs.any { it.type.typeName == "org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Type<?>>" }) {
                listOf(
                    Result(
                        "tuple",
                        data,
                        "unable to parse dynamic arrays, the whole result is: ${data}, maybe you can deduce what it means"
                    )
                )
            } else {
                throw it
            }

        }
    }

    data class Result(
        val type: String,
        val value: Any?,
        val error: String? = null
    )
}