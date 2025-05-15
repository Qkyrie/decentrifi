package fi.decentri.evm.model

import org.web3j.abi.datatypes.Function

data class ContractCall(
    val function: Function,
    val address: String
)