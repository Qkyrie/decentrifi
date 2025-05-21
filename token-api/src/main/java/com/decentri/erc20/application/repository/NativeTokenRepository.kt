package com.decentri.erc20.application.repository

import com.decentri.erc20.application.LogoGenerator
import com.decentri.erc20.domain.FungibleToken
import fi.decentri.evm.Chain
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class NativeTokenRepository(
    private val logoGenerator: LogoGenerator
) {

    fun getNativeToken(network: Chain): FungibleToken {
        return when (network) {
            Chain.ETHEREUM -> eth(network)
        }
    }

    private fun eth(network: Chain) = createToken(
        network = network,
        name = "ETH",
        symbol = "ETH",
    )

    private fun createToken(
        address: String = nullAddress,
        network: Chain,
        name: String,
        decimals: Int = 18,
        symbol: String,
    ) = FungibleToken(
        logo = logoGenerator.generate(network, "0x0"),
        address = address,
        name = name,
        decimals = decimals,
        symbol = symbol,
        network = network,
        totalSupply = BigInteger.ZERO,
        verified = true
    )
}

val nullAddress = "0x0"