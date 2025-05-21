package com.decentri.erc20.port.output

import com.decentri.erc20.LPTokenContract
import fi.decentri.evm.Chain


interface ReadLPPort {
    suspend fun getLP(network: Chain, address: String): LPTokenContract
}