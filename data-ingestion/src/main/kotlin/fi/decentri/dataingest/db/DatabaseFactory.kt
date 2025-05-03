package fi.decentri.dataingest.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fi.decentri.dataingest.config.DatabaseConfig
import fi.decentri.dataingest.model.Contracts
import fi.decentri.dataingest.model.IngestionMetadata
import fi.decentri.dataingest.model.RawInvocations
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Factory for database connections and initialization
 */
object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun init(config: DatabaseConfig) {
        logger.info("Initializing database connection: ${config.jdbcUrl}")

        // Configure HikariCP connection pool
        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = config.jdbcUrl
            username = config.username
            password = config.password
            maximumPoolSize = config.maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        // Create the datasource and initialize the database
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        // Initialize the schema and metadata
        transaction {
            SchemaUtils.createMissingTablesAndColumns(RawInvocations)
            SchemaUtils.createMissingTablesAndColumns(IngestionMetadata)
            SchemaUtils.createMissingTablesAndColumns(Contracts)
        }
    }

    /**
     * Helper function for running database transactions in a coroutine context
     */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
