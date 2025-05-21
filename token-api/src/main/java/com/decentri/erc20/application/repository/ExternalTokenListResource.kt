package com.decentri.erc20.application.repository

import fi.decentri.evm.Chain

interface ExternalTokenListResource {
    fun allTokens(network: Chain): List<ExternalToken>
}