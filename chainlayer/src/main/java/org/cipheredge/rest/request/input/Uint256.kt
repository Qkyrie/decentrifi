package org.cipheredge.rest.request.input

import fi.decentri.evm.TypeUtils.Companion.toUint256
import java.math.BigInteger

class Uint256(
    type: String,
    value: BigInteger
) : Input<BigInteger>(type, value) {
    override fun typed() = value.toUint256()
}