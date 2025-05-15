package org.cipheredge.rest.request.input

import org.bouncycastle.util.encoders.Hex
import org.cipheredge.web3j.TypeUtils.Companion.toBytes4

class Bytes4(
    type: String,
    value: ByteArray
) : Input<ByteArray>(type, value) {
    override fun typed() = Hex.decode(value).toBytes4()
}