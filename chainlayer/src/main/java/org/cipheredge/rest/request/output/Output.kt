package org.cipheredge.rest.request.output

import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Type

class Output(val type: String, val indexed: Boolean?) {
    fun makeTypeReference(): TypeReference<out Type<*>> {
        if (type == "tuple") {
            return object : TypeReference<DynamicArray<Type<*>>>() {}
        }
        return TypeReference.makeTypeReference(this.type, indexed ?: false, false)
    }
}