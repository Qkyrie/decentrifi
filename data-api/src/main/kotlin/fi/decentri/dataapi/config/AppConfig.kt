package fi.decentri.dataapi.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File

/**
 * Application configuration that loads from application.conf
 */
class AppConfig private constructor(config: Config) {
    val server = ServerConfig(config.getConfig("server"))
    val database = DatabaseConfig(config.getConfig("database"))
    
    companion object {
        fun load(): AppConfig {
            // Load configuration in the following order of priority:
            // 1. Command line arguments
            // 2. Environment variables
            // 3. application.conf file
            val config = ConfigFactory.systemProperties()
                .withFallback(ConfigFactory.systemEnvironment())
                .withFallback(
                    try {
                        val configFile = File("application.conf")
                        if (configFile.exists()) {
                            ConfigFactory.parseFile(configFile)
                        } else {
                            ConfigFactory.load()
                        }
                    } catch (e: Exception) {
                        ConfigFactory.load()
                    }
                )
                .resolve()
            
            return AppConfig(config)
        }
    }
}

class ServerConfig(config: Config) {
    val port: Int = config.getInt("port")
}

class DatabaseConfig(config: Config) {
    val jdbcUrl: String = config.getString("jdbcUrl")
    val username: String = config.getString("username")
    val password: String = config.getString("password")
    val maxPoolSize: Int = config.getInt("maxPoolSize")
}