package org.cipheredge.rest.request.input

import org.cipheredge.web3j.TypeUtils.Companion.toBool


class Bool(
    type: String,
    value: Boolean
) : Input<Boolean>(type, value) {
    override fun typed() = value.toBool()
}