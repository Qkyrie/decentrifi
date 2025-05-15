package org.cipheredge.rest.request.input

import org.bouncycastle.util.encoders.Hex
import org.cipheredge.web3j.TypeUtils.Companion.toBytes32

class Bytes32(
    type: String,
    value: ByteArray
) : Input<ByteArray>(type, value) {
    override fun typed() = Hex.decode(value).toBytes32()
}