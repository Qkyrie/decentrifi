package fi.decentri.abi

import org.slf4j.LoggerFactory
import org.web3j.protocol.ObjectMapperFactory
import org.web3j.protocol.core.methods.response.AbiDefinition
import java.io.IOException

/**
 * Service for handling ABI (Application Binary Interface) related operations
 */
class AbiService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapperFactory.getObjectMapper()

    /**
     * Parse an ABI string and extract functions and events.
     *
     * @param abiString The ABI JSON string to parse
     * @return A pair containing lists of extracted functions and events
     */
    fun parseABI(abiString: String): Pair<List<AbiFunction>, List<AbiEvent>> {
        try {
            logger.debug("Parsing ABI string")

            // Parse the ABI JSON string to an array of AbiDefinition objects
            val abiDefinitions = objectMapper.readValue(
                abiString,
                Array<AbiDefinition>::class.java
            )

            // Extract functions and events
            val functions = abiDefinitions
                .filter { it.type == "function" }
                .map { abiDef ->
                    AbiFunction(
                        name = abiDef.name ?: "",
                        inputs = abiDef.inputs?.map {
                            AbiFunctionParameter(it.name ?: "", it.type ?: "", it.isIndexed)
                        } ?: emptyList(),
                        outputs = abiDef.outputs?.map {
                            AbiFunctionParameter(it.name ?: "", it.type ?: "", it.isIndexed)
                        } ?: emptyList(),
                        stateMutability = abiDef.stateMutability ?: "",
                        constant = abiDef.isConstant,
                        payable = abiDef.isPayable
                    )
                }

            val events = abiDefinitions
                .filter { it.type == "event" }
                .map { abiDef ->
                    AbiEvent(
                        name = abiDef.name ?: "",
                        inputs = abiDef.inputs?.map {
                            AbiFunctionParameter(it.name ?: "", it.type ?: "", it.isIndexed)
                        } ?: emptyList(),
                    )
                }

            logger.info("Successfully parsed ABI: found ${functions.size} functions and ${events.size} events")
            return Pair(functions, events)
        } catch (e: IOException) {
            logger.error("Failed to parse ABI string: ${e.message}", e)
            throw IllegalArgumentException("Invalid ABI JSON format", e)
        } catch (e: Exception) {
            logger.error("Error while parsing ABI: ${e.message}", e)
            throw e
        }
    }
}


