package fi.decentri.dataapi.repository

import fi.decentri.dataapi.model.Contract
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.contract.Contracts
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

/**
 * Repository for managing contract data in the data-api module
 */
@ExperimentalTime
class ContractsRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Insert a new contract
     */
    suspend fun insert(address: String, abi: String, network: String): Int {
        return dbQuery {
            Contracts.insert {
                it[this.address] = address
                it[this.abi] = abi
                it[chain] = network
                it[name] = null
            }[Contracts.id]
        }
    }

    /**
     * Find a contract by address and network
     */
    suspend fun findByAddressAndNetwork(address: String, network: String): Contract? {
        return dbQuery {
            Contracts.select { (Contracts.address eq address) and (Contracts.chain eq network) }
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
        )
    }
}