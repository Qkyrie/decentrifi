package org.cipheredge.rest.request.input

import fi.decentri.evm.TypeUtils.Companion.toBytes32
import org.bouncycastle.util.encoders.Hex

class Bytes32(
    type: String,
    value: ByteArray
) : Input<ByteArray>(type, value) {
    override fun typed() = Hex.decode(value).toBytes32()
}