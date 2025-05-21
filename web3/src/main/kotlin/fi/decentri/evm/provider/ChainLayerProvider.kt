package fi.decentri.evm.provider

import fi.decentri.evm.Chain
import fi.decentri.evm.MultiCallCaller
import fi.decentri.evm.MultiCallV2Caller
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides ChainLayer instances for different blockchain networks.
 * Acts as a registry for different blockchain network gateways.
 */
class ChainLayerProvider {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val chainLayers = ConcurrentHashMap<Chain, ChainLayer>()

    companion object {
        private val INSTANCE = ChainLayerProvider()

        const val ETHEREUM_ENDPOINT = "https://data.decentri.fi/chains/ethereum"


        val endpoints = mapOf(
            Chain.ETHEREUM to ETHEREUM_ENDPOINT,
        )


        fun getInstance(): ChainLayerProvider = INSTANCE
    }

    /**
     * Creates a standard HTTP client configured for blockchain API calls
     */
    private fun createHttpClient(): HttpClient {
        return HttpClient(Apache) {
            install(ContentNegotiation) {
                // Content negotiation setup
            }
        }
    }


    /**
     * Gets or creates a ChainLayer for the specified network
     */
    fun getChainLayer(network: Chain): ChainLayer {
        return chainLayers.computeIfAbsent(network) { createChainLayer(it) }
    }

    fun getEvmChainLayer(network: Chain): EvmGateway {
        return getChainLayer(network) as EvmGateway
    }

    /**
     * Creates a new ChainLayer instance for the specified network
     */
    private fun createChainLayer(network: Chain): ChainLayer {
        logger.info("Creating ChainLayer for network: $network")

        val httpClient = createHttpClient()
        val multiCallCaller = MultiCallV2Caller(network.multicallAddress)
        return EvmGateway(
            network = network,
            multicallCaller = multiCallCaller,
            httpClient = httpClient,
            endpoint = endpoints[network]!!
        )
    }
}