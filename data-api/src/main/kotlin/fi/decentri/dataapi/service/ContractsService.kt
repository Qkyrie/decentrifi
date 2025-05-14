package fi.decentri.dataapi.service

import fi.decentri.dataapi.model.Contract
import fi.decentri.dataapi.repository.ContractsRepository
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

/**
 * Service for managing contract data including ABI and address
 */
@ExperimentalTime
class ContractsService(
    private val contractsRepository: ContractsRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    /**
     * Get a contract by ID
     */
    suspend fun getContract(id: Int): Contract? {
        return contractsRepository.getById(id)
    }
}
