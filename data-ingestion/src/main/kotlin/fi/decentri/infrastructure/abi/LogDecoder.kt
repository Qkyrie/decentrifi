package fi.decentri.infrastructure.abi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.web3j.abi.EventEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.*
import org.web3j.protocol.core.methods.response.Log
import org.web3j.tx.Contract

/**
 * Data class to hold decoded log information including the event name.
 */
data class DecodedLog(
    val eventName: String,
    val parameters: Map<String, Any?>
)

object LogDecoder {

    fun Log.decode(abi: String): DecodedLog? {
        val logger = LoggerFactory.getLogger(this::class.java)
        try {
            // Use LogDecoder to decode the event data
            val decoded = decodeLog(this, abi)
            return decoded ?: run {
                logger.debug("LogDecoder returned empty map for event, falling back to simple decoding")
                null
            }
        } catch (e: Exception) {
            logger.error("Error decoding event data with LogDecoder: ${e.message}", e)
            // Fallback to simpler decoding if LogDecoder throws an exception
            return null
        }
    }

    /**
     * Decode a single log using the contract ABI (supplied as JSON string).
     *
     * @return DecodedLog object containing the event name and decoded parameters,
     *         or null if the log does not match any event in the ABI.
     */
    fun decodeLog(log: Log, abiJson: String): DecodedLog? {
        val mapper = ObjectMapper()
        val abiNodes = mapper.readTree(abiJson)

        for (eventJson in abiNodes) {
            if (eventJson["type"]?.asText() != "event") continue

            val eventName = eventJson["name"].asText()
            val inputsJson = eventJson["inputs"]

            val indexedRefs = mutableListOf<TypeReference<out Type<*>>>()
            val nonIdxRefs = mutableListOf<TypeReference<out Type<*>>>()
            val inputNames = mutableListOf<Pair<String, Boolean>>()          // name + indexed?

            // build the TypeReference lists
            inputsJson.forEach { input ->
                val isIndexed = input["indexed"].asBoolean()
                val ref = buildTypeReference(input, isIndexed)

                if (isIndexed) indexedRefs.add(ref) else nonIdxRefs.add(ref)
                inputNames.add(input["name"].asText() to isIndexed)
            }

            val event = org.web3j.abi.datatypes.Event(eventName, indexedRefs + nonIdxRefs)
            val signature = EventEncoder.encode(event)

            // Skip if this log isn't for the current event
            if (log.topics.isEmpty() || log.topics[0] != signature) continue

            val values = Contract.staticExtractEventParameters(event, log) ?: continue

            // Merge indexed + non‑indexed back into the original order
            val result = mutableMapOf<String, Any?>()
            var idxIdx = 0
            var nonIdx = 0

            inputNames.forEach { (name, isIndexed) ->
                val value = if (isIndexed) values.indexedValues[idxIdx++] else values.nonIndexedValues[nonIdx++]
                result[name] = value.value  // unwrap the abi Type<T>
            }
            return DecodedLog(eventName, result)
        }
        return null
    }


    @Suppress("UNCHECKED_CAST")
    private fun buildTypeReference(input: JsonNode, indexed: Boolean): TypeReference<out Type<*>> {
        val solidityType = input["type"].asText()

        // ---------- tuple & tuple[] ----------
        if (solidityType.startsWith("tuple")) {
            val components = input["components"]
            val componentRefs = components.map { buildTypeReference(it, false) }

            fun structRef(idx: Boolean) =
                object : TypeReference<DynamicStruct>(idx, componentRefs) {}

            return if (solidityType.endsWith("[]")) {
                object : TypeReference<DynamicArray<DynamicStruct>>(indexed) {
                    override fun getSubTypeReference() = structRef(false)
                }
            } else {
                structRef(indexed)
            }
        }

        // ---------- scalar types ----------
        val cls: Class<out Type<*>> = when {
            solidityType == "address" -> Address::class.java
            solidityType == "bool" -> Bool::class.java
            solidityType == "string" -> Utf8String::class.java
            solidityType == "bytes" -> DynamicBytes::class.java
            solidityType.startsWith("bytes") -> Class.forName(
                "org.web3j.abi.datatypes.generated.Bytes${solidityType.removePrefix("bytes")}"
            ) as Class<out Type<*>>

            solidityType.startsWith("uint") -> Class.forName(
                "org.web3j.abi.datatypes.generated.Uint${solidityType.removePrefix("uint")}"
            ) as Class<out Type<*>>

            solidityType.startsWith("int") -> Class.forName(
                "org.web3j.abi.datatypes.generated.Int${solidityType.removePrefix("int")}"
            ) as Class<out Type<*>>

            else -> throw IllegalArgumentException("Unsupported Solidity type: $solidityType")
        }
        return TypeReference.create(cls as Class<Type<*>>, indexed)
    }


}