package com.decentri.erc20.adapter.contract

import arrow.core.Either.Companion.catch
import arrow.core.Option
import com.decentri.erc20.ERC20
import com.decentri.erc20.adapter.tokens.NATIVE_WRAP_MAPPING
import com.decentri.erc20.port.output.ReadERC20Port
import fi.decentri.evm.Chain
import fi.decentri.evm.ERC20Contract
import fi.decentri.evm.model.MultiCallResult
import fi.decentri.evm.provider.ChainLayerProvider
import fi.decentri.evm.provider.EvmGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
private class ERC20ContractReader(
) : ReadERC20Port {

    val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun getERC20(network: Chain, address: String): Option<ERC20> {
        return catch {
            val correctAddress =
                if (address == "0x0" || address == "0x0000000000000000000000000000000000000000") NATIVE_WRAP_MAPPING[network]!! else address
            val contract = ChainLayerProvider.getInstance().getEvmChainLayer(network).contractAt {
                ERC20Contract(correctAddress)
            }
            val result = contract.fetchERC20Information()
            ERC20(
                name = getValue(result.name, contract::readName),
                symbol = getValue(result.symbol, contract::readSymbol),
                decimals = getValue<BigInteger>(result.decimals, contract::readDecimals).toInt(),
                network = network,
                address = correctAddress.lowercase(),
                totalSupply = getValue<BigInteger>(result.totalSupply) {
                    contract.readTotalSupply()
                }
            )
        }.mapLeft {
            logger.error("Error creating ERC20 contract for $address on ${network.slug}: ", it.message)
        }.getOrNone()
    }

    private suspend inline fun <reified T> getValue(
        result: MultiCallResult,
        default: suspend () -> T
    ): T {
        return if (result.success) {
            if (result.data.isEmpty()) {
                throw IllegalStateException("No data in result")
            }
            result.data.first().value as T
        } else {
            default()
        }
    }
}