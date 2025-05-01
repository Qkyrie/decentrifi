# Data Ingestion Module

This module is responsible for ingesting blockchain data from a specific contract (DAI on Ethereum Mainnet by default) and storing it in a PostgreSQL database with TimescaleDB.

## Features

- Continuously polls blockchain for new contract invocations
- Tracks and processes ERC-20 transfer events
- Decodes function signatures and input parameters
- Stores decoded invocations in TimescaleDB
- Tracks last processed block for resuming after restarts
- Configurable batch size and polling interval
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

### Database Schema

The data is stored in two main tables:

1. `raw_invocations` - Stores decoded contract invocations
2. `ingestion_metadata` - Stores metadata like the last processed block

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

### ingestion_metadata

| Column | Type        | Description                   |
|--------|-------------|-------------------------------|
| key    | VARCHAR(64) | Metadata key (primary key)    |
| value  | VARCHAR(64) | Metadata value                |

## Customization

To ingest data from a different contract:

1. Update the `ethereum.contractAddress` configuration
2. Modify the `IngestorService.kt` to handle the specific events and functions of the target contract

## Extending

The current implementation focuses on ERC-20 token transfers. To extend for other contract types:

1. Add new function signatures in `IngestorService.kt`
2. Implement appropriate decoding logic in the `parseFunctionInput` method
3. Add any additional event types you want to track
