@file:OptIn(ExperimentalTime::class, ExperimentalTime::class)

package fi.decentri.dataingest.service

import fi.decentri.dataingest.ingest.EventIngestorService
import fi.decentri.dataingest.ingest.RawInvocationIngestorService
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

class BlockchainIngestor(
    private val contractsService: ContractsService,
    private val rawInvocationIngestorService: RawInvocationIngestorService,
    private val eventIngestorService: EventIngestorService,
    parentScope: CoroutineScope  // inject an application/lifecycle scope
) {

    private val scope = parentScope + SupervisorJob(parentScope.coroutineContext[Job])

    private val logger = LoggerFactory.getLogger(BlockchainIngestor::class.java)

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
                    logger.info("Starting ingestion for contract: ${contract.address} (${contract.name ?: "unnamed"}) on chain: ${contract.chain}")

                    // Launch raw invocations ingestion
               /*     launch {
                        try {
                            rawInvocationIngestorService.ingest(contract)
                            logger.info("Raw invocations ingestion complete for contract ${contract.address}: caught up with the latest block")
                        } catch (e: Exception) {
                            logger.error(
                                "Error during raw invocations ingestion for contract ${contract.address} (ID: ${contract.id}): ${e.message}",
                                e
                            )
                        }
                    } */

                    // Launch events ingestion
                    launch {
                        try {
                            eventIngestorService.ingest(contract)
                            logger.info("Events ingestion complete for contract ${contract.address}: caught up with the latest block")
                        } catch (e: Exception) {
                            logger.error(
                                "Error during events ingestion for contract ${contract.address} (ID: ${contract.id}): ${e.message}",
                                e
                            )
                        }
                    }
                }
            }
        }
    }
}