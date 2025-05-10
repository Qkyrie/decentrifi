package fi.decentri.dataingest.config

import com.typesafe.config.Config
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Manager for Web3j instances across multiple blockchain networks.
 * Loads configuration from application.conf and provides Web3j instances on demand.
 */
class Web3jManager private constructor(config: Config) {
    private val logger = LoggerFactory.getLogger(Web3jManager::class.java)
    private val networks = ConcurrentHashMap<String, NetworkConfig>()
    private val web3jInstances = ConcurrentHashMap<String, Web3>()
    private val mutex = Mutex()
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(4)
    
    init {
        // Load network configurations
        try {
            // Add Ethereum network by default for backward compatibility
            if (config.hasPath("ethereum")) {
                val ethConfig = NetworkConfig(
                    rpcUrl = config.getString("ethereum.rpcUrl"),
                    batchSize = if (config.hasPath("ethereum.batchSize")) config.getInt("ethereum.batchSize") else 1000,
                    eventBatchSize = if (config.hasPath("ethereum.eventBatchSize")) config.getInt("ethereum.eventBatchSize") else 2000,
                    pollingInterval = if (config.hasPath("ethereum.pollingInterval")) config.getLong("ethereum.pollingInterval") else 15000,
                    blockTime = 12 // Default Ethereum block time in seconds
                )
                networks["ethereum"] = ethConfig
            }
            
            // Load networks from the "networks" section if it exists
            if (config.hasPath("networks")) {
                val networksConfig = config.getConfig("networks")
                // Get all root keys in the networks config section
                networksConfig.root().keys.forEach { networkName ->
                    if (networksConfig.hasPath(networkName)) {
                        val networkConfig = networksConfig.getConfig(networkName)
                        val network = NetworkConfig(
                            rpcUrl = networkConfig.getString("rpcUrl"),
                            batchSize = if (networkConfig.hasPath("batchSize")) networkConfig.getInt("batchSize") else 1000,
                            eventBatchSize = if (networkConfig.hasPath("eventBatchSize")) networkConfig.getInt("eventBatchSize") else 2000,
                            pollingInterval = if (networkConfig.hasPath("pollingInterval")) networkConfig.getLong("pollingInterval") else 15000,
                            blockTime = if (networkConfig.hasPath("blockTime")) networkConfig.getInt("blockTime") else 12 // Default to Ethereum block time
                        )
                        networks[networkName] = network
                    }
                }
            }
            
            if (networks.isEmpty()) {
                logger.warn("No blockchain networks configured. Please check your application.conf")
            } else {
                logger.info("Loaded configuration for ${networks.size} blockchain networks: ${networks.keys.joinToString()}")
            }
        } catch (e: Exception) {
            logger.error("Error loading network configurations: ${e.message}", e)
        }
    }
    
    suspend fun web3(network: String): Web3? {
        // Return existing instance if available
        if (web3jInstances.containsKey(network)) {
            return web3jInstances[network]
        }
        
        // Create new instance if network exists
        return mutex.withLock {
            // Double-check after acquiring the lock
            if (web3jInstances.containsKey(network)) {
                return@withLock web3jInstances[network]
            }
            
            val networkConfig = networks[network] ?: return@withLock null.also {
                logger.error("Network '$network' is not configured in application.conf")
            }
            
            try {
                val httpService = HttpService(
                    networkConfig.rpcUrl,
                    OkHttpClient.Builder()
                        .connectTimeout(20.seconds.toJavaDuration())
                        .build()
                )
                
                val web3 = Web3(
                    Web3j.build(httpService, 2_000L, scheduler),
                    networkConfig,
                    httpService
                )
                web3jInstances[network] = web3
                logger.info("Created Web3j instance for network: $network")
                web3
            } catch (e: Exception) {
                logger.error("Failed to create Web3j instance for network '$network': ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Gets the network configuration for the specified network.
     * 
     * @param network The network name
     * @return The network configuration, or null if not found
     */
    fun getNetworkConfig(network: String): NetworkConfig? {
        return networks[network]
    }
    
    /**
     * Gets all configured network names.
     * 
     * @return Set of network names
     */
    fun getNetworkNames(): Set<String> {
        return networks.keys
    }
    
    /**
     * Shuts down all Web3j instances and releases resources.
     */
    fun shutdown() {
        web3jInstances.values.forEach { it.web3j.shutdown() }
        web3jInstances.clear()
        scheduler.shutdown()
        logger.info("Shut down all Web3j instances")
    }
    
    companion object {
        private var instance: Web3jManager? = null
        private val initMutex = Mutex()
        
        /**
         * Initializes the Web3jManager with the provided configuration.
         * 
         * @param config The application configuration
         * @return The Web3jManager instance
         */
        suspend fun init(config: Config): Web3jManager {
            return initMutex.withLock {
                if (instance == null) {
                    instance = Web3jManager(config)
                }
                instance!!
            }
        }
        
        /**
         * Gets the Web3jManager instance. Must call init() first.
         * 
         * @return The Web3jManager instance
         * @throws IllegalStateException if the manager has not been initialized
         */
        fun getInstance(): Web3jManager {
            return instance ?: throw IllegalStateException("Web3jManager has not been initialized. Call init() first.")
        }
    }
    
    /**
     * Configuration for a blockchain network.
     */
    data class NetworkConfig(
        val rpcUrl: String,
        val batchSize: Int,
        val eventBatchSize: Int,
        val pollingInterval: Long,
        val blockTime: Int // Average block time in seconds
    )

    data class Web3(
        val web3j: Web3j,
        val networkConfig: NetworkConfig,
        val httpService: HttpService
    )
}