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

/**
 * Data structure for counting events per hour
 */
@Serializable
data class HourlyEventCount(
    val hour: Int,
    val count: Int
)

/**
 * Data structure for events per hour by type
 */
@Serializable
data class EventTypeHourlyData(
    val eventName: String,
    val hourlyCounts: List<HourlyEventCount>
)

/**
 * Response object for hourly event data
 */
@Serializable
data class HourlyEventsResponseDTO(
    val network: String,
    val contract: String,
    val eventTypes: List<EventTypeHourlyData>,
    val from: Instant,
    val to: Instant
)

/**
 * Response object for decoded event keys
 */
@Serializable
data class DecodedKeysResponseDTO(
    val network: String,
    val contract: String,
    val eventKeys: Map<String, List<String>> // Map of event name to list of decoded parameter names
)