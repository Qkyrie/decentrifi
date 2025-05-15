package org.cipheredge.rest

import org.cipheredge.chain.Chain
import org.cipheredge.chain.evm.config.Web3jHolder
import org.cipheredge.chain.evm.web3j.Web3JProxyImpl
import org.cipheredge.rest.request.GetEventLogsRequest
import org.springframework.web.bind.annotation.*
import org.web3j.protocol.core.methods.response.EthLog

@RestController
@RequestMapping("/{chain}/events")
class NativeLogsRestController(
    private val web3JProxyImpl: Web3JProxyImpl,
    private val web3jHolder: Web3jHolder,
) {

    @PostMapping("/logs")
    suspend fun getEvents(
        @PathVariable("chain") chain: String,
        @RequestBody getEventLogsRequest: GetEventLogsRequest
    ): EthLog {
        require(getEventLogsRequest.addresses.isNotEmpty()) { "Address must not be empty" }
        val n = Chain.fromString(chain)
        return when {
            n.isEVM -> {
                web3JProxyImpl.getLogs(getEventLogsRequest, web3jHolder.getWeb3j(n))
            }

            else -> {
                throw IllegalArgumentException("Unsupported chain: $chain")
            }
        }
    }
}