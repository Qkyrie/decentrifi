package fi.decentri.dataingest.service

import fi.decentri.dataingest.model.Contract
import fi.decentri.infrastructure.abi.AbiService
import fi.decentri.infrastructure.repository.contract.ContractsRepository
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

/**
 * Service for managing contract data including ABI and address
 */
@ExperimentalTime
class ContractsService(
    private val contractsRepository: ContractsRepository,
    private val abiService: AbiService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)


    /**
     * Get a contract by ID
     */
    suspend fun getContract(id: Int): Contract? {
        return contractsRepository.getById(id)
    }

    /**
     * Find contracts by address
     */
    suspend fun findContractsByAddress(address: String): List<Contract> {
        return contractsRepository.findByAddress(address)
    }

    /**
     * Find contracts by chain
     */
    suspend fun findContractsByChain(chain: String): List<Contract> {
        return contractsRepository.findByChain(chain)
    }

    /**
     * Find a contract by address and chain
     */
    suspend fun findContractByAddressAndChain(address: String, chain: String): Contract? {
        return contractsRepository.findByAddressAndChain(address, chain)
    }

    /**
     * Get all contracts
     */
    suspend fun getAllContracts(): List<Contract> {
        return contractsRepository.getAll()
    }

    /**
     * Delete a contract
     */
    suspend fun deleteContract(id: Int): Boolean {
        logger.info("Deleting contract with ID: $id")
        return contractsRepository.delete(id)
    }
}
