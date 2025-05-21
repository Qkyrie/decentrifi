package com.decentri.erc20.adapter.contract

import com.decentri.erc20.LPTokenContract
import com.decentri.erc20.port.output.ReadLPPort
import fi.decentri.evm.Chain
import fi.decentri.evm.provider.ChainLayerProvider
import io.github.reactivecircus.cache4k.Cache
import org.springframework.stereotype.Component
import java.util.*

@Component
private class LpContractReader(
) : ReadLPPort {

    private val cache = Cache.Builder<String, LPTokenContract>().build()

    override suspend fun getLP(network: Chain, address: String): LPTokenContract {
        val key = "${network.name}-${address.lowercase(Locale.getDefault())}"
        return cache.get(key) {
            ChainLayerProvider.getInstance().getEvmChainLayer(network).contractAt {
                LPTokenContract(address)
            }
        }
    }
}