package fi.decentri.dataapi.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Data transfer object for blockchain events
 */
@Serializable
data class EventDTO(
    val contractAddress: String,
    val txHash: String,
    val logIndex: Int,
    val blockNumber: Long,
    val blockTimestamp: Instant,
    val eventName: String?,
    val topics: List<String>,
    val data: String?,
    val decoded: Map<String, Any>?
)

/**
 * Response object for a collection of events
 */
@Serializable
data class EventsResponseDTO(
    val network: String,
    val contract: String,
    val events: List<EventDTO>,
    val from: Instant,
    val to: Instant
)