@file:OptIn(ExperimentalTime::class, ExperimentalTime::class)

package fi.decentri.dataingest.service

import fi.decentri.dataingest.ingest.IngestorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

class BlockchainIngestor(
    private val contractsService: ContractsService,
    private val ingestorService: IngestorService,
    private val coroutineScope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(BlockchainIngestor::class.java)

    fun startIngestion() {
        // Start the blockchain data ingestion process in a background coroutine
        coroutineScope.launch(Dispatchers.IO) {
            logger.info("Starting blockchain data ingestion service with trace_filter")

            // Fetch all contracts from the database
            val contracts = contractsService.getAllContracts()

            if (contracts.isEmpty()) {
                logger.warn("No contracts found in the database. Ingestion will not start.")
            } else {
                logger.info("Found ${contracts.size} contracts in the database")

                // Start ingestion for each contract address
                contracts.forEach { contract ->
                    logger.info("Starting ingestion for contract: ${contract.address} (${contract.name ?: "unnamed"}) on chain: ${contract.chain}")
                    launch(Dispatchers.IO) {
                        try {
                            ingestorService.ingest(contract)
                            logger.info("Ingestion complete for contract ${contract.address} (ID: ${contract.id}): caught up with the latest block")
                        } catch (e: Exception) {
                            logger.error(
                                "Error during ingestion for contract ${contract.address} (ID: ${contract.id}): ${e.message}",
                                e
                            )
                        }
                    }
                }
            }
        }
    }
}