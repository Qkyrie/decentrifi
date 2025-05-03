package fi.decentri.db.config

/**
 * Interface for database configuration parameters
 */
interface DatabaseConfig {
    /**
     * JDBC URL for database connection
     */
    val jdbcUrl: String
    
    /**
     * Database username
     */
    val username: String
    
    /**
     * Database password
     */
    val password: String
    
    /**
     * Maximum connection pool size
     */
    val maxPoolSize: Int
}