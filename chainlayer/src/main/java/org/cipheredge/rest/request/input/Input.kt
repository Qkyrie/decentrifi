package org.cipheredge.rest.request.input

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.web3j.abi.datatypes.Type

@JsonSubTypes(
    JsonSubTypes.Type(value = Uint256::class, name = "uint256"),
    JsonSubTypes.Type(value = Uint24::class, name = "uint24"),
    JsonSubTypes.Type(value = Address::class, name = "address"),
    JsonSubTypes.Type(value = InputString::class, name = "string"),
    JsonSubTypes.Type(value = InputString::class, name = "string"),
    JsonSubTypes.Type(value = Bytes32::class, name = "bytes32"),
    JsonSubTypes.Type(value = Bytes4::class, name = "bytes4"),
    JsonSubTypes.Type(value = Bool::class, names = ["bool", "boolean"]),
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, visible = true, property = "type")
abstract class Input<T>(
    val type: String,
    val value: T
) {
    abstract fun typed(): Type<T>
}

