@file:OptIn(ExperimentalTime::class, ExperimentalTime::class)

package fi.decentri.dataingest.service

import fi.decentri.application.usecases.EventIngestorUseCase
import fi.decentri.application.usecases.IngestRawInvocationsUseCase
import fi.decentri.application.usecases.TokenTransferListenerUseCase
import fi.decentri.dataingest.model.Contract
import fi.decentri.dataingest.model.MetadataType
import fi.decentri.infrastructure.repository.ingestion.IngestionMetadataRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.time.ExperimentalTime

class IngestionAutoMode(
    private val contractsService: ContractsService,
    private val ingestRawInvocationsUseCase: IngestRawInvocationsUseCase,
    private val eventIngestorUseCase: EventIngestorUseCase,
    private val tokenTransferListenerUseCase: TokenTransferListenerUseCase,
    parentScope: CoroutineScope,  // inject an application/lifecycle scope
) {

    companion object {
        private val logger = LoggerFactory.getLogger(IngestionAutoMode::class.java)
    }

    private val scope = parentScope + SupervisorJob(parentScope.coroutineContext[Job])
    private val metadataRepository = IngestionMetadataRepository()

    /**
     * Start ingestion for all contracts in auto mode
     */
    fun startIngestion(): Job = scope.launch {
        // Start the blockchain data ingestion process in a background coroutine
        launch(Dispatchers.IO) {
            logger.info("Starting blockchain data ingestion service (raw invocations and events)")

            // Fetch all contracts from the database
            val contracts = contractsService.getAllContracts()

            if (contracts.isEmpty()) {
                logger.warn("No contracts found in the database. Ingestion will not start.")
            } else {
                logger.info("Found ${contracts.size} contracts in the database")

                // Start ingestion for each contract address
                contracts.forEach { contract ->
                    // Check if the contract was recently processed
                    if (shouldProcessContract(contract)) {
                        logger.info("Starting ingestion for contract: ${contract.address} (${contract.name ?: "unnamed"}) on chain: ${contract.chain}")

                        // Launch raw invocations ingestion
                        launch {
                            try {
                                ingestRawInvocationsUseCase.invoke(contract)
                                logger.info("Raw invocations ingestion complete for contract ${contract.address}: caught up with the latest block")
                            } catch (e: Exception) {
                                logger.error(
                                    "Error during raw invocations ingestion for contract ${contract.address} (ID: ${contract.id}): ${e.message}",
                                    e
                                )
                            }
                        }

                        // Launch events ingestion
                        launch {
                            try {
                                eventIngestorUseCase.ingest(contract)
                                logger.info("Events ingestion complete for contract ${contract.address}: caught up with the latest block")
                            } catch (e: Exception) {
                                logger.error(
                                    "Error during events ingestion for contract ${contract.address} (ID: ${contract.id}): ${e.message}",
                                    e
                                )
                            }
                        }

                        // Launch token transfer tracking for 'safe' type contracts
                        if (contract.type == "safe") {
                            launch {
                                try {
                                    tokenTransferListenerUseCase.listenForTransfers(contract)
                                    logger.info("Token transfer ingestion complete for contract ${contract.address}")
                                } catch (e: Exception) {
                                    logger.error(
                                        "Error during token transfer ingestion for contract ${contract.address} (ID: ${contract.id}): ${e.message}",
                                        e
                                    )
                                }
                            }
                        }
                    } else {
                        logger.info("Skipping contract ${contract.address} (${contract.name ?: "unnamed"}) as it was recently processed")
                    }
                }
            }
        }
    }

    /**
     * Determine if a contract should be processed based on when it was last processed
     * We skip processing if the contract was processed within the auto mode cooldown period
     */
    private suspend fun shouldProcessContract(contract: Contract): Boolean {
        // Get the last run timestamp for raw invocations
        val lastRunRawString = metadataRepository.getMetadatForContractId(
            MetadataType.RAW_INVOCATIONS_LAST_RUN_TIMESTAMP, contract.id!!
        )

        // Get the last run timestamp for events
        val lastRunEventsString = metadataRepository.getMetadatForContractId(
            MetadataType.EVENTS_LAST_RUN_TIMESTAMP, contract.id
        )

        // If we've never processed this contract, we should definitely process it
        if (lastRunRawString == null && lastRunEventsString == null) {
            return true
        }

        val now = Instant.now()

        // Check the raw invocations timestamp if it exists
        if (lastRunRawString != null) {
            try {
                val lastRunTime = Instant.parse(lastRunRawString)
                val timeSinceLastRun = java.time.Duration.between(lastRunTime, now)

                if (timeSinceLastRun < MetadataType.AUTO_MODE_COOLDOWN) {
                    logger.info(
                        "Contract ${contract.address} was processed ${timeSinceLastRun.toMinutes()} minutes ago, " + "which is less than the cooldown period of ${MetadataType.AUTO_MODE_COOLDOWN.toMinutes()} minutes"
                    )
                    return false
                }
            } catch (e: Exception) {
                logger.warn("Could not parse last run timestamp for raw invocations: $lastRunRawString", e)
                // If we can't parse the timestamp, assume we should process the contract
            }
        }

        // Check the events timestamp if it exists
        if (lastRunEventsString != null) {
            try {
                val lastRunTime = Instant.parse(lastRunEventsString)
                val timeSinceLastRun = java.time.Duration.between(lastRunTime, now)

                if (timeSinceLastRun < MetadataType.AUTO_MODE_COOLDOWN) {
                    logger.info(
                        "Contract ${contract.address} events were processed ${timeSinceLastRun.toMinutes()} minutes ago, " + "which is less than the cooldown period of ${MetadataType.AUTO_MODE_COOLDOWN.toMinutes()} minutes"
                    )
                    return false
                }
            } catch (e: Exception) {
                logger.warn("Could not parse last run timestamp for events: $lastRunEventsString", e)
                // If we can't parse the timestamp, assume we should process the contract
            }
        }

        return true
    }
}