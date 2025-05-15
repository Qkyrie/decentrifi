package fi.decentri.evm

import fi.decentri.evm.model.ContractCall
import fi.decentri.evm.model.MultiCallResult
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type

interface MultiCallCaller {

    suspend fun readMultiCall(
        elements: List<ContractCall>,
        executeCall: suspend (address: String, function: Function) -> List<Type<*>>
    ): List<MultiCallResult>
}