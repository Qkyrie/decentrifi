package fi.decentri.evm

import arrow.core.nonEmptyListOf
import fi.decentri.evm.model.ContractCall
import fi.decentri.evm.model.MultiCallResult
import fi.decentri.evm.TypeUtils.Companion.string
import fi.decentri.evm.provider.EvmGateway.Companion.createFunction
import fi.decentri.evm.TypeUtils.Companion.toAddress
import fi.decentri.evm.TypeUtils.Companion.uint256
import org.web3j.abi.datatypes.Function
import java.math.BigInteger

open class ERC20Contract(
    address: String
) : EvmContract(address) {

    companion object {
        fun balanceOf(address: String): Function {
            return createFunction(
                "balanceOf",
                inputs = listOf(address.toAddress()),
                outputs = listOf(uint256())
            )
        }
    }


    fun balanceOfFunction(address: String): ContractCall {
        return createFunction(
            "balanceOf",
            inputs = listOf(address.toAddress()),
            outputs = listOf(uint256())
        )
    }

    suspend fun fetchERC20Information(): ERC20MulticallResult {
        val result = readMultiCall(
            nonEmptyListOf(
                createFunction("name", outputs = listOf(string())),
                createFunction("symbol", outputs = listOf(string())),
                createFunction("decimals", outputs = listOf(uint256())),
                createFunction("totalSupply", outputs = listOf(uint256())),
            )
        )

        return ERC20MulticallResult(
            name = result[0],
            symbol = result[1],
            decimals = result[2],
            totalSupply = result[3],
        )
    }

    data class ERC20MulticallResult(
        val name: MultiCallResult,
        val symbol: MultiCallResult,
        val decimals: MultiCallResult,
        val totalSupply: MultiCallResult,
    )


    suspend fun readAllowance(owner: String, spender: String): BigInteger {
        return read(
            "allowance",
            listOf(owner.toAddress(), spender.toAddress()),
            listOf(uint256())
        )[0].value as BigInteger
    }


    suspend fun balanceOf(address: String): BigInteger {
        val retVal = read(
            "balanceOf",
            inputs = listOf(address.toAddress()),
            outputs = listOf(uint256())
        )
        return if (retVal.isEmpty()) {
            logger.debug("Unable to fetch balance of on {} for {}", getNetwork(), address)
            BigInteger.ZERO
        } else {
            retVal[0].value as BigInteger
        }
    }


    suspend fun readName(): String {
        return try {
            readSingle("name", string())
        } catch (ex: Exception) {
            logger.error("ERC20: Error reading name for token $address on ${getNetwork()}")
            "unknown"
        }
    }

    suspend fun readSymbol(): String {
        return try {
            readSingle("symbol", string())
        } catch (ex: Exception) {
            "UNKWN"
        }
    }

    suspend fun readDecimals(): BigInteger {
        return try {
            readSingle("decimals", uint256())
        } catch (ex: Exception) {
            BigInteger.valueOf(18)
        }
    }


    @Suppress("UNCHECKED_CAST")
    suspend fun readTotalSupply(): BigInteger {
        return try {
            readSingle("totalSupply", uint256())
        } catch (ex: Exception) {
            BigInteger.ZERO
        }
    }
}