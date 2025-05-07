package fi.decentri.dataapi.model

/**
 * DTO for unique addresses response
 */
data class UniqueAddressesDTO(
    val network: String,
    val contract: String,
    val uniqueAddressCount: Long,
    val periodHours: Int = 24 // Default to 24 hours
)