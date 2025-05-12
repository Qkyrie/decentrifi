package fi.decentri.infrastructure.abi

/**
 * Data class representing an event in the ABI
 */
data class AbiEvent(
    val name: String,
    val inputs: List<AbiFunctionParameter>,
)