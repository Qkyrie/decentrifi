package com.decentri.erc20.application.repository

import fi.decentri.evm.Chain

data class TokenListResponse(
    var tokens: List<TokenListEntry>
)

data class TokenListEntry(
    val chainId: Int,
    val name: String,
    val symbol: String,
    val logoURI: String?,
    val address: String,
    val extensions: Extensions?,
    val decimals: Int,
) {


    fun getExtensionAddresses(): List<ExternalToken> {
        val network = Chain.fromChainId(chainId) ?: throw IllegalArgumentException("Unknown chainId $chainId")
        return extensions?.bridgeInfo?.entries?.map {
            ExternalToken(
                address = address,
                symbol = symbol,
                name = name,
                network = network,
                logoURI = logoURI ?: "",
                decimals = decimals
            )
        } ?: emptyList()
    }

    fun accumulate(): List<ExternalToken> {
        val extensionAddresses = getExtensionAddresses()

        val network = Chain.fromChainId(chainId) ?: throw IllegalArgumentException("Unknown chainId $chainId")

        return extensionAddresses + listOf(
            ExternalToken(
                address = address,
                symbol = symbol,
                name = name,
                network = network,
                logoURI = logoURI ?: "",
                decimals = decimals
            )
        )
    }
}

data class Extensions(
    val bridgeInfo: Map<String, BridgeInfo>?
)

data class BridgeInfo(
    val tokenAddress: String
)