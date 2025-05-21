package com.decentri.erc20.application

import fi.decentri.evm.Chain
import org.springframework.stereotype.Component
import org.web3j.crypto.Keys

@Component
class LogoGenerator {

    fun generate(network: Chain, address: String): String {
        return when {
            address == "0x0" -> {
                "https://github.com/Qkyrie/decentrifi-data/raw/master/logo/native-tokens/${network.slug}.png"
            }

            network.isEVM -> {
                "https://raw.githubusercontent.com/Qkyrie/assets/master/blockchains/${network.slug}/assets/${
                    Keys.toChecksumAddress(
                        address
                    )
                }/logo.png"
            }

            else -> {
                "https://raw.githubusercontent.com/Qkyrie/assets/master/blockchains/${network.slug}/assets/${address}/logo.png"
            }
        }
    }
}