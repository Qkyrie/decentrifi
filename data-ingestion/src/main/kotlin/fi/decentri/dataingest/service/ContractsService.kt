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
     * Create a new contract
     */
    suspend fun createContract(address: String, abi: String, chain: String, name: String? = null, type: String? = null): Contract {
        logger.info("Creating new contract: address=$address, chain=$chain, type=${type ?: "generic"}")

        // Validate ABI format by parsing it
        try {
            abiService.parseABI(abi)
        } catch (e: Exception) {
            logger.error("Invalid ABI format for contract $address: ${e.message}")
            throw IllegalArgumentException("Invalid ABI format: ${e.message}", e)
        }

        // If type is null, default to "generic"
        val contractType = type ?: "generic"

        val contract = Contract(
            address = address,
            abi = abi,
            chain = chain,
            name = name,
            type = contractType,
            createdAt = kotlin.time.Clock.System.now(),
            updatedAt = kotlin.time.Clock.System.now()
        )

        val id = contractsRepository.insert(contract)
        logger.info("Created contract with ID: $id, type: $contractType")

        return contract.copy(id = id)
    }

    /**
     * Update an existing contract
     */
    suspend fun updateContract(id: Int, address: String, abi: String, chain: String, name: String? = null, type: String? = null): Contract? {
        logger.info("Updating contract ID: $id")

        // Validate ABI format by parsing it
        try {
            abiService.parseABI(abi)
        } catch (e: Exception) {
            logger.error("Invalid ABI format for contract update (ID: $id): ${e.message}")
            throw IllegalArgumentException("Invalid ABI format: ${e.message}", e)
        }

        val existingContract = contractsRepository.getById(id)
        if (existingContract == null) {
            logger.warn("Contract not found with ID: $id")
            return null
        }

        // Determine the type - keep the existing one if not specified, otherwise use the new one or default to "generic"
        val contractType = when {
            type != null -> type
            existingContract.type != null -> existingContract.type
            else -> "generic"
        }

        val updatedContract = existingContract.copy(
            address = address,
            abi = abi,
            chain = chain,
            name = name,
            type = contractType
        )

        val success = contractsRepository.update(updatedContract)
        if (!success) {
            logger.warn("Failed to update contract with ID: $id")
            return null
        }

        logger.info("Updated contract with ID: $id, type: $contractType")
        return updatedContract
    }

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
