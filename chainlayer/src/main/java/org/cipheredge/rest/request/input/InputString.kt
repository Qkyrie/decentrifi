package org.cipheredge.rest.request.input

import org.cipheredge.web3j.TypeUtils.Companion.toUtf8String


class InputString(
    type: String,
    value: String
) : Input<String>(type, value) {
    override fun typed() = value.toUtf8String()
}