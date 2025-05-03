package fi.decentri.abi

/**
 * Data class representing a parameter for a function or event
 */
data class AbiFunctionParameter(
    val name: String,
    val type: String,
    val indexed: Boolean
)
