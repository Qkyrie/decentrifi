package fi.decentri.dataapi.model

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@ExperimentalTime
data class Contract(
    val id: Int? = null,
    val address: String,
    val abi: String,
    val chain: String,
    val name: String? = null,
    val createdAt: kotlin.time.Instant = Clock.System.now(),
    val updatedAt: kotlin.time.Instant = Clock.System.now()
)