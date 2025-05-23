package fi.decentri.dataapi.service

import fi.decentri.dataapi.model.*
import fi.decentri.dataapi.repository.RawLogEntry
import fi.decentri.dataapi.repository.RawLogsRepository
import fi.decentri.dataapi.routes.JsonbFilter
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for handling blockchain event data
 */
class EventService(private val rawLogsRepository: RawLogsRepository) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Get hourly event counts from the last 24 hours for a specific contract on a given network
     * Returns data aggregated by hour and event type
     */
    suspend fun getHourlyEventCounts(
        network: String,
        contract: String,
        filters: List<JsonbFilter> = emptyList()        // default = no filter
    ): HourlyEventsResponseDTO {
        logger.info("Calculating hourly event counts for network=$network, contract=$contract")

        // Get raw data from repository
        val rawEvents = rawLogsRepository.getEventsFromLast24Hours(network, contract, filters)

        // Calculate time range
        val now = Instant.now()
        val oneDayAgo = now.minus(24, ChronoUnit.HOURS)

        // Convert to DTOs first
        val eventDTOs = rawEvents.map { convertToDTO(it) }

        // Process events by type and hour
        val eventsByType = eventDTOs.groupBy { it.eventName ?: "Unknown" }
        val hourlyDataByType = eventsByType.map { (eventName, events) ->
            // Create 24 hour slots from 24 hours ago until now
            val hourSlots = mutableListOf<Instant>()
            for (hourOffset in 23 downTo 0) {
                hourSlots.add(now.minus(hourOffset.toLong(), ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS))
            }

            // Create a map to hold the count for each hour slot
            val hourlyCounts = mutableMapOf<Instant, Int>()

            // Initialize all hour slots with zero counts
            hourSlots.forEach { hourSlot ->
                hourlyCounts[hourSlot] = 0
            }

            // Count events by hour slot
            events.forEach { event ->
                // Find the matching hour slot for this event
                val eventHourTimestamp = event.blockTimestamp.truncatedTo(ChronoUnit.HOURS)
                if (hourlyCounts.containsKey(eventHourTimestamp)) {
                    hourlyCounts[eventHourTimestamp] = hourlyCounts.getOrDefault(eventHourTimestamp, 0) + 1
                }
            }

            // Convert to our DTO structure
            val hourlyCountsList = hourlyCounts.entries
                .sortedBy { it.key }
                .map { HourlyEventCount(it.key, it.value) }

            EventTypeHourlyData(
                eventName = eventName,
                hourlyCounts = hourlyCountsList
            )
        }

        return HourlyEventsResponseDTO(
            network = network,
            contract = contract,
            eventTypes = hourlyDataByType,
            from = oneDayAgo,
            to = now
        )
    }

    /**
     * Get all unique decoded keys for events from a specific contract
     * Returns a map of event names to their decoded parameter keys
     */
    suspend fun getDecodedEventKeys(network: String, contract: String): DecodedKeysResponseDTO {
        logger.info("Getting decoded event keys for network=$network, contract=$contract")

        val eventKeys = rawLogsRepository.getDecodedEventKeys(network, contract)

        return DecodedKeysResponseDTO(
            network = network,
            contract = contract,
            eventKeys = eventKeys
        )
    }

    private fun JsonElement.toAny(): Any? = when (this) {
        JsonNull -> null

        is JsonPrimitive -> when {
            isString -> content                     // "foo"
            booleanOrNull != null -> boolean                     // true / false
            longOrNull != null -> long                        // 42
            doubleOrNull != null -> double                      // 3.14
            else -> content                   // fallback as String
        }

        is JsonArray -> map { it.toAny() }                        // List<Any?>

        is JsonObject -> mapValues { (_, v) -> v.toAny() }         // Map<String, Any?>
    }

    /**
     * Convert internal repository data to DTO
     */
    private fun convertToDTO(entry: RawLogEntry): EventDTO {
        // Extract decoded fields as a Map
        @Suppress("UNCHECKED_CAST")
        val decodedMap = try {
            entry.decoded.toAny() as? Map<String, Any>
        } catch (e: Exception) {
            logger.warn("Failed to parse decoded JSON for event ${entry.txHash}:${entry.logIndex}", e)
            null
        }

        // Convert JsonElement topics to List<String>
        val topicsList = try {
            if (entry.topics is JsonArray) {
                entry.topics.map { it.jsonPrimitive.content }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse topics JSON for event ${entry.txHash}:${entry.logIndex}", e)
            emptyList()
        }

        return EventDTO(
            contractAddress = entry.contractAddress,
            txHash = entry.txHash,
            logIndex = entry.logIndex,
            blockNumber = entry.blockNumber,
            blockTimestamp = entry.blockTimestamp,
            eventName = entry.eventName,
            topics = topicsList,
            data = entry.data,
            decoded = decodedMap
        )
    }
}