# Data Ingestion Module

This module is responsible for ingesting blockchain data from specific contracts across multiple blockchain networks (Ethereum Mainnet by default) and storing it in a PostgreSQL database with TimescaleDB.

## Features

- Continuously polls blockchain for new contract invocations
- Supports multiple blockchain networks (Ethereum, Polygon, Arbitrum, etc.)
- Tracks and processes contract events
- Decodes function signatures and input parameters
- Stores decoded invocations and events in TimescaleDB
- Tracks last processed block for resuming after restarts
- Configurable batch size and polling interval
- Network-specific configuration for RPC endpoints and blockchain parameters
- Built with Kotlin, Ktor, and Exposed ORM

## Getting Started

### Prerequisites

- JDK 21
- Maven 3.8+
- Docker and Docker Compose (for local development)
- PostgreSQL with TimescaleDB extension
- Ethereum RPC endpoint (e.g., Infura, Alchemy)

### Configuration

The application is configured via `application.conf` which supports overriding via environment variables:

```hocon
server {
    port = 8080
    port = ${?SERVER_PORT}
}

database {
    jdbcUrl = "jdbc:postgresql://localhost:5432/decentrifi"
    jdbcUrl = ${?DB_JDBC_URL}
    username = "postgres"
    username = ${?DB_USERNAME}
    password = "postgres"
    password = ${?DB_PASSWORD}
    maxPoolSize = 10
    maxPoolSize = ${?DB_MAX_POOL_SIZE}
}

# Legacy Ethereum config (maintained for backward compatibility)
ethereum {
    # Ethereum Mainnet RPC endpoint
    rpcUrl = "https://mainnet.infura.io/v3/YOUR_INFURA_KEY"
    rpcUrl = ${?ETH_RPC_URL}

    # DAI token contract address on Ethereum Mainnet
    contractAddress = "0x6b175474e89094c44da98b954eedeac495271d0f"
    contractAddress = ${?ETH_CONTRACT_ADDRESS}

    # Block to start ingestion from (set to 0 to use the value from DB)
    startBlock = 0
    startBlock = ${?ETH_START_BLOCK}

    # Number of blocks to process in one batch
    batchSize = 100
    batchSize = ${?ETH_BATCH_SIZE}

    # Polling interval in milliseconds
    pollingInterval = 15000
    pollingInterval = ${?ETH_POLLING_INTERVAL}
}

# Multi-network configuration
networks {
    # Ethereum Mainnet
    ethereum {
        rpcUrl = "https://mainnet.infura.io/v3/YOUR_INFURA_KEY"
        rpcUrl = ${?ETH_RPC_URL}

        # Contract-specific settings
        contractAddress = "0x6b175474e89094c44da98b954eedeac495271d0f"
        contractAddress = ${?ETH_CONTRACT_ADDRESS}
        startBlock = 0
        startBlock = ${?ETH_START_BLOCK}

        # Ingestion settings
        batchSize = 100
        batchSize = ${?ETH_BATCH_SIZE}
        eventBatchSize = 2000
        eventBatchSize = ${?ETH_EVENT_BATCH_SIZE}
        pollingInterval = 15000
        pollingInterval = ${?ETH_POLLING_INTERVAL}

        # Network characteristics
        blockTime = 12  # Average block time in seconds
    }

    # Example of additional network configuration
    # polygon {
    #     rpcUrl = "https://polygon-rpc.com"
    #     rpcUrl = ${?POLYGON_RPC_URL}
    #     batchSize = 500
    #     eventBatchSize = 1000
    #     pollingInterval = 5000
    #     blockTime = 2  # Average block time in seconds
    # }
}
```

### Running Locally

1. Start the database:

```bash
cd docker
docker-compose up -d postgres
```

2. Build the application:

```bash
mvn clean package
```

3. Run the application:

```bash
cd data-ingestion
java -jar target/data-ingestion-0.0.1-SNAPSHOT.jar
```

### Running with Docker Compose

```bash
cd docker
docker-compose up -d
```

### Running with Different Networks

The application now supports multiple blockchain networks. You can specify which network to use when running in contract-specific mode:

```bash
# Ingest data for a specific contract on Ethereum
java -jar target/data-ingestion-0.0.1-SNAPSHOT.jar --mode=contract --contract=0x6b175474e89094c44da98b954eedeac495271d0f --network=ethereum

# Ingest data for a contract on Polygon
java -jar target/data-ingestion-0.0.1-SNAPSHOT.jar --mode=contract --contract=0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174 --network=polygon
```

Each network can have its own configuration in the `networks` section of the application.conf file, including:

- RPC URL
- Batch sizes
- Polling intervals
- Block time characteristics

For more details on the multi-network configuration, see the [WEB3J_MANAGER.md](WEB3J_MANAGER.md) documentation.

### Database Schema

The data is stored in several main tables:

1. `raw_invocations` - Stores decoded contract invocations
2. `raw_logs` - Stores decoded contract events
3. `event_definitions` - Stores ABI information for contract events
4. `ingestion_metadata` - Stores metadata like the last processed block

## Database Schema Details

### raw_invocations

| Column            | Type          | Description                              |
|-------------------|---------------|------------------------------------------|
| id                | SERIAL        | Primary key                              |
| network           | VARCHAR(64)   | Blockchain network (e.g., "ethereum")    |
| contract_address  | VARCHAR(42)   | Contract address                         |
| block_number      | BIGINT        | Block number                             |
| block_timestamp   | TIMESTAMP     | Block timestamp                          |
| tx_hash           | VARCHAR(66)   | Transaction hash                         |
| from_address      | VARCHAR(42)   | Sender address                           |
| function_selector | VARCHAR(10)   | Function selector (e.g., "0xa9059cbb")   |
| function_name     | VARCHAR(64)   | Function name (e.g., "transfer")         |
| input_args        | JSONB         | Decoded function arguments               |
| status            | BOOLEAN       | Transaction success status               |
| gas_used          | BIGINT        | Gas used by the transaction              |

### raw_logs

| Column            | Type          | Description                              |
|-------------------|---------------|------------------------------------------|
| id                | SERIAL        | Primary key                              |
| network           | VARCHAR(64)   | Blockchain network (e.g., "ethereum")    |
| contract_address  | VARCHAR(42)   | Contract address                         |
| tx_hash           | VARCHAR(66)   | Transaction hash                         |
| log_index         | INTEGER       | Log index within transaction             |
| block_number      | BIGINT        | Block number                             |
| block_timestamp   | TIMESTAMP     | Block timestamp                          |
| topic_0           | VARCHAR(66)   | Event signature (first topic)            |
| topics            | TEXT[]        | All topics in event                      |
| data              | TEXT          | Raw event data                           |
| event_name        | VARCHAR(100)  | Decoded event name from ABI              |
| decoded           | JSONB         | Parsed event parameters                  |

### event_definitions

| Column            | Type          | Description                              |
|-------------------|---------------|------------------------------------------|
| id                | SERIAL        | Primary key                              |
| contract_address  | VARCHAR(42)   | Contract address                         |
| event_name        | VARCHAR(100)  | Event name                               |
| signature         | VARCHAR(66)   | Event signature hash                     |
| abi_json          | TEXT          | JSON fragment of the event ABI           |
| network           | VARCHAR(50)   | Blockchain network                       |
| created_at        | TIMESTAMP     | Creation timestamp                       |
| updated_at        | TIMESTAMP     | Last update timestamp                    |

### ingestion_metadata

| Column | Type        | Description                   |
|--------|-------------|-------------------------------|
| key    | VARCHAR(64) | Metadata key (primary key)    |
| value  | VARCHAR(64) | Metadata value                |

## Customization

### Changing Contracts

To ingest data from a different contract:

1. Update the contract address in your network configuration:
   - For backward compatibility: `ethereum.contractAddress`
   - For multi-network: `networks.<network_name>.contractAddress`
2. The system will automatically ingest both raw invocations and events from the contract

### Adding New Networks

To add support for a new blockchain network:

1. Add a new section to the `networks` configuration in application.conf:
   ```hocon
   networks {
       # Existing networks...

       # New network
       optimism {
           rpcUrl = "https://mainnet.optimism.io"
           rpcUrl = ${?OPTIMISM_RPC_URL}
           batchSize = 1000
           eventBatchSize = 2000
           pollingInterval = 5000
           blockTime = 2  # Average block time in seconds
       }
   }
   ```
2. Run the application with the `--network=optimism` parameter when ingesting data from contracts on that network

## Extending

The current implementation can handle any contract with a valid ABI across multiple blockchain networks. Both raw invocations (using trace_filter) and events are automatically ingested and decoded based on the contract's ABI.

The ingestion architecture includes:

1. `Web3jManager` - Manages Web3j instances across multiple networks
2. `BlockService` - Provides network-aware blockchain querying capabilities
3. `RawInvocationIngestorService` - Handles all contract function calls
4. `EventIngestorService` - Handles all contract events
5. `BlockchainIngestor` - Orchestrates the ingestion process

### Extension Points

1. **Adding Network-Specific Processing Logic**
   - Extend the ingestor services to handle network-specific quirks
   - Implement network-specific decoders for special contract types

2. **Customizing the Decoding Process**
   - To modify how function calls or events are decoded, update the respective ingestor service
   - Network-specific parameters can be accessed via the `Web3jManager`

3. **Supporting New Network Types**
   - Add custom network configuration in application.conf
   - Implement any special RPC methods needed for that network

4. **Adding Cross-Chain Analytics**
   - Use data from multiple networks to create cross-chain analytics
   - Query the database with network as a filter parameter