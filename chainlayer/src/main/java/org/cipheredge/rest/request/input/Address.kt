package org.cipheredge.rest.request.input

import fi.decentri.evm.TypeUtils.Companion.toAddress

class Address(
    type: String,
    value: String
) : Input<String>(type, value) {
    override fun typed() = value.toAddress()
}