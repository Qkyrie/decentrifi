package fi.decentri.dataapi.repository

import fi.decentri.dataapi.model.Contract
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.contract.Contracts
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

/**
 * Repository for managing contract data in the data-api module
 */
@ExperimentalTime
class ContractsRepository {

    /**
     * Get a contract by ID
     */
    suspend fun getById(id: Int): Contract? {
        return dbQuery {
            Contracts.selectAll().where { Contracts.id eq id }
                .map { toContract(it) }
                .singleOrNull()
        }
    }

    /**
     * Insert a new contract
     */
    suspend fun insert(address: String, abi: String, network: String, type: String? = null): Int {
        return dbQuery {
            // Check if the contract already exists
            val existingContract = findByAddressAndNetwork(address, network)
            if (existingContract != null) {
                // If it exists, return the existing contract ID
                return@dbQuery existingContract.id!!
            }

            // If type is null, default to "generic"
            val contractType = type ?: "generic"

            Contracts.insert {
                it[this.address] = address
                it[this.abi] = abi
                it[chain] = network
                it[name] = null
                it[this.type] = contractType
            }[Contracts.id]
        }
    }

    /**
     * Find a contract by address and network
     */
    suspend fun findByAddressAndNetwork(address: String, network: String): Contract? {
        return dbQuery {
            Contracts.selectAll().where { (Contracts.address eq address.lowercase()) and (Contracts.chain eq network) }
                .map { toContract(it) }
                .singleOrNull()
        }
    }

    /**
     * Convert a ResultRow to a Contract
     */
    private fun toContract(row: ResultRow): Contract {
        return Contract(
            id = row[Contracts.id],
            address = row[Contracts.address],
            abi = row[Contracts.abi],
            chain = row[Contracts.chain],
            name = row[Contracts.name],
            type = row[Contracts.type] ?: "generic"
        )
    }
}