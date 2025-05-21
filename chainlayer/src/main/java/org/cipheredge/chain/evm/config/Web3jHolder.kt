package org.cipheredge.chain.evm.config

import okhttp3.OkHttpClient
import fi.decentri.evm.Chain
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.util.concurrent.TimeUnit

private fun buildWeb3jService(url: String): HttpService {
    val builder = OkHttpClient.Builder()
    builder.connectTimeout(20, TimeUnit.SECONDS)
    builder.writeTimeout(60, TimeUnit.SECONDS)
    builder.readTimeout(60, TimeUnit.SECONDS)
    builder.callTimeout(60, TimeUnit.SECONDS)
    return HttpService(url, false)
}

class Web3jHolder(
    val web3js: List<Web3>
) {
    fun getWeb3j(chain: Chain): Web3j {
        return web3js.firstOrNull { it.chain == chain }?.web3j
            ?: throw IllegalArgumentException("No Web3j found for chain: ${chain.slug}")
    }
}

data class Web3(
    val chain: Chain,
    private val endpoint: String,
) {

    val web3j = Web3j.build(buildWeb3jService(endpoint))
}

