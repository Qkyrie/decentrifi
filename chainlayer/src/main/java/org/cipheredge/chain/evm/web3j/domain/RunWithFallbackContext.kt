package org.cipheredge.chain.evm.web3j.domain

import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response

data class RunWithFallbackContext<T : Response<*>>(
    val maxTries: Int = 3,
    val tries: Int = 0,
    val requestProvider: () -> Request<*, T>,
) {

    constructor(
        requestProvider: () -> Request<*, T>,
    ) : this(3, 0, requestProvider)

    fun increment(): RunWithFallbackContext<T> {
        val newTries = this.tries + 1
        if (newTries > maxTries) {
            throw ExchaustedRetriesException()
        }
        return this.copy(tries = newTries)
    }
}

class ExchaustedRetriesException : RuntimeException()