package fi.decentri.dataapi.model

/**
 * Data class for handling contract submission from the frontend
 */
data class ContractSubmission(
    val contractAddress: String,
    val network: String,
    val abi: String
)