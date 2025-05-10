# Web3j Manager

The Web3jManager is a class designed to manage multiple Web3j instances for different blockchain networks. It provides a convenient way to access Web3j instances for different networks based on configuration in application.conf.

## Configuration

Networks are configured in the `application.conf` file. Each network has its own configuration section:

```hocon
# Multi-network configuration
networks {
    # Ethereum Mainnet
    ethereum {
        rpcUrl = "https://mainnet.infura.io/v3/YOUR_INFURA_KEY"
        rpcUrl = ${?ETH_RPC_URL}
        batchSize = 1000
        eventBatchSize = 2000
        pollingInterval = 15000
        blockTime = 12  # Average block time in seconds
    }
    
    # Polygon Mainnet
    polygon {
        rpcUrl = "https://polygon-rpc.com"
        rpcUrl = ${?POLYGON_RPC_URL}
        batchSize = 500
        eventBatchSize = 1000
        pollingInterval = 5000
        blockTime = 2  # Average block time in seconds
    }
    
    # Add more networks as needed
}
```

Each network configuration includes:

- `rpcUrl`: The RPC endpoint URL for the network
- `batchSize`: Number of blocks to process in one batch for raw invocations
- `eventBatchSize`: Number of blocks to process in one batch for events
- `pollingInterval`: Polling interval in milliseconds
- `blockTime`: Average block time in seconds

For backward compatibility, the legacy `ethereum` section is still supported.

## Usage

### Initialization

The Web3jManager must be initialized before use:

```kotlin
// Initialize with the application configuration
val web3jManager = Web3jManager.init(ConfigFactory.load())
```

Or if you already have the config object:

```kotlin
val web3jManager = Web3jManager.init(config)
```

### Getting a Web3j Instance

To get a Web3j instance for a specific network:

```kotlin
val web3j = web3jManager.web3j("ethereum") // Returns Web3j instance for Ethereum

// For other networks
val polygonWeb3j = web3jManager.web3j("polygon")
```

### Access Network Configuration

To get the configuration for a specific network:

```kotlin
val ethConfig = web3jManager.getNetworkConfig("ethereum")
println("Ethereum block time: ${ethConfig?.blockTime} seconds")
```

### List Available Networks

To get all configured network names:

```kotlin
val networks = web3jManager.getNetworkNames()
println("Configured networks: ${networks.joinToString()}")
```

### Shutdown

When you're done with the Web3jManager, make sure to shut it down to release resources:

```kotlin
web3jManager.shutdown()
```

## Integration with BlockService

The BlockService can be initialized with the Web3jManager:

```kotlin
val blockService = BlockService(web3jManager)
```

Then, when using BlockService methods, specify the network:

```kotlin
// Get latest block for Ethereum
val latestEthBlock = blockService.getLatestBlock("ethereum")

// Get latest block for Polygon
val latestPolygonBlock = blockService.getLatestBlock("polygon")
```

## Thread Safety

The Web3jManager is thread-safe and can be accessed from multiple threads or coroutines concurrently.