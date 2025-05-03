package fi.decentri.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fi.decentri.db.config.DatabaseConfig
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Factory for database connections and initialization
 */
object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Initialize the database connection
     * 
     * @param config Database configuration
     */
    fun init(config: DatabaseConfig) {
        logger.info("Initializing database connection: ${config.jdbcUrl}")

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
        
        logger.info("Database initialized successfully")
    }
    
    /**
     * Initialize database tables
     * 
     * @param tables Tables to create if they don't exist
     */
    fun initTables(vararg tables: Table) {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(*tables)
        }
    }

    /**
     * Helper function for running database transactions in a coroutine context
     */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}