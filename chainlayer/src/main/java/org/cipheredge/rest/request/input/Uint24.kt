package org.cipheredge.rest.request.input

import fi.decentri.evm.TypeUtils.Companion.toUint24
import java.math.BigInteger

class Uint24(
    type: String,
    value: BigInteger
) : Input<BigInteger>(type, value) {
    override fun typed() = value.toUint24()
}