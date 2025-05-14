package fi.decentri.dataingest.model

import java.time.Instant
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Data class representing a contract with its ABI and address
 */
@ExperimentalTime
data class Contract(
    val id: Int? = null,
    val address: String,
    val abi: String,
    val chain: String,
    val name: String? = null,
    val type: String,
    val createdAt: kotlin.time.Instant = Clock.System.now(),
    val updatedAt: kotlin.time.Instant = Clock.System.now()
)
