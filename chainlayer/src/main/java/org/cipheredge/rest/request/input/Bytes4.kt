package org.cipheredge.rest.request.input

import fi.decentri.evm.TypeUtils.Companion.toBytes4
import org.bouncycastle.util.encoders.Hex

class Bytes4(
    type: String,
    value: ByteArray
) : Input<ByteArray>(type, value) {
    override fun typed() = Hex.decode(value).toBytes4()
}