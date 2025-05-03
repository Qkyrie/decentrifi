package fi.decentri.abi
/**
 * Data class representing a function in the ABI
 */
data class AbiFunction(
    val name: String,
    val inputs: List<AbiFunctionParameter>,
    val outputs: List<AbiFunctionParameter>,
    val stateMutability: String,
    val constant: Boolean,
    val payable: Boolean
)
