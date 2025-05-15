package fi.decentri.evm.provider

import fi.decentri.evm.EvmContract
import fi.decentri.evm.model.ContractCall
import fi.decentri.evm.model.MultiCallResult

class BulkConstantResolver(
    private val gateway: EvmGateway
) {

    suspend fun <T : EvmContract> resolve(contracts: List<T>): List<T> {
        val evmContracts = contracts.groupBy {
            it.getNetwork()
        }

        evmContracts.forEach { (network, contracts) ->
            resolveForNetwork(contracts, network)
        }

        return contracts
    }

    private suspend fun resolveForNetwork(contracts: List<EvmContract>, network: String) {
        val functionByContracts = contracts.flatMap { contract ->
            contract.constantFunctions.map { function ->
                function to contract
            }
        }

        val multicallResults = gateway.readMultiCall(
            functionByContracts.map {
                it.first
            }
        )

        val s = multicallResults.mapIndexed { index, result ->
            val pair = functionByContracts[index]
            ConstantResult(
                pair.first,
                result,
                pair.second
            )
        }.groupBy {
            it.contract
        }.forEach {
            it.key.resolveConstants(
                it.value.associate { constantResult ->
                    constantResult.function to constantResult.result
                }
            )
        }
    }

    data class ConstantResult(
        val function: ContractCall,
        val result: MultiCallResult,
        val contract: EvmContract
    )
}