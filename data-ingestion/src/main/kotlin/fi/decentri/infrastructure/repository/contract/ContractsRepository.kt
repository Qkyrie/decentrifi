package fi.decentri.infrastructure.repository.contract

import fi.decentri.dataingest.model.Contract
import fi.decentri.db.DatabaseFactory
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.contract.Contracts
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

/**
 * Repository for managing contract data including ABI and address
 */
@ExperimentalTime
class ContractsRepository {

    /**
     * Insert a new contract
     */
    suspend fun insert(contract: Contract): Int {
        return dbQuery {
            Contracts.insert {
                it[address] = contract.address
                it[abi] = contract.abi
                it[chain] = contract.chain
                it[name] = contract.name
                it[type] = contract.type
            }[Contracts.id]
        }
    }

    /**
     * Update an existing contract
     */
    suspend fun update(contract: Contract): Boolean {
        if (contract.id == null) {
            throw IllegalArgumentException("Cannot update contract without ID")
        }

        return dbQuery {
            val rowsUpdated = Contracts.update({ Contracts.id eq contract.id }) {
                it[address] = contract.address
                it[abi] = contract.abi
                it[chain] = contract.chain
                it[name] = contract.name
                it[type] = contract.type
            }
            rowsUpdated > 0
        }
    }

    /**
     * Get a contract by ID
     */
    suspend fun getById(id: Int): Contract? {
        return dbQuery {
            Contracts.select { Contracts.id eq id }
                .map { toContract(it) }
                .singleOrNull()
        }
    }

    /**
     * Find contracts by address
     */
    suspend fun findByAddress(address: String): List<Contract> {
        return dbQuery {
            Contracts.select { Contracts.address eq address }
                .map { toContract(it) }
        }
    }

    /**
     * Find contracts by chain
     */
    suspend fun findByChain(chain: String): List<Contract> {
        return dbQuery {
            Contracts.selectAll().where { Contracts.chain eq chain }
                .map { toContract(it) }
        }
    }

    /**
     * Find a contract by address and chain
     */
    suspend fun findByAddressAndChain(address: String, chain: String): Contract? {
        return dbQuery {
            Contracts.select { (Contracts.address eq address) and (Contracts.chain eq chain) }
                .map { toContract(it) }
                .singleOrNull()
        }
    }

    /**
     * Get all contracts
     */
    suspend fun getAll(): List<Contract> {
        return dbQuery {
            Contracts.selectAll()
                .map { toContract(it) }
        }
    }

    /**
     * Delete a contract by ID
     */
    suspend fun delete(id: Int): Boolean {
        return dbQuery {
            val rowsDeleted = Contracts.deleteWhere { Contracts.id eq id }
            rowsDeleted > 0
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