package com.decentri.erc20.port.output

import arrow.core.Option
import com.decentri.erc20.ERC20
import fi.decentri.evm.Chain

interface ReadERC20Port {
    suspend fun getERC20(network: Chain, address: String): Option<ERC20>
}