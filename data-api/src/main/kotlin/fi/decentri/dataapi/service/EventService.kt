package fi.decentri.dataapi.service

import fi.decentri.dataapi.model.EventDTO
import fi.decentri.dataapi.model.EventsResponseDTO
import fi.decentri.dataapi.repository.RawLogsRepository
import fi.decentri.dataapi.repository.RawLogEntry
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
     * Get all events from the last 24 hours for a specific contract on a given network
     */
    suspend fun getEventsFromLast24Hours(network: String, contract: String): EventsResponseDTO {
        logger.info("Fetching events from last 24 hours for network=$network, contract=$contract")
        
        // Get raw data from repository
        val rawEvents = rawLogsRepository.getEventsFromLast24Hours(network, contract)
        
        // Calculate time range
        val now = Instant.now()
        val oneDayAgo = now.minus(24, ChronoUnit.HOURS)
        
        // Convert to DTOs
        val eventDTOs = rawEvents.map { convertToDTO(it) }
        
        return EventsResponseDTO(
            network = network,
            contract = contract,
            events = eventDTOs,
            from = oneDayAgo,
            to = now
        )
    }

    private fun JsonElement.toAny(): Any? = when (this) {
        JsonNull           -> null

        is JsonPrimitive -> when {
            isString                     -> content                     // "foo"
            booleanOrNull     != null    -> boolean                     // true / false
            longOrNull        != null    -> long                        // 42
            doubleOrNull      != null    -> double                      // 3.14
            else                           -> content                   // fallback as String
        }

        is JsonArray       -> map { it.toAny() }                        // List<Any?>

        is JsonObject      -> mapValues { (_, v) -> v.toAny() }         // Map<String, Any?>
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
        
        return EventDTO(
            contractAddress = entry.contractAddress,
            txHash = entry.txHash,
            logIndex = entry.logIndex,
            blockNumber = entry.blockNumber,
            blockTimestamp = entry.blockTimestamp,
            eventName = entry.eventName,
            topics = entry.topics,
            data = entry.data,
            decoded = decodedMap
        )
    }
}