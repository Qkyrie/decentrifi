# decentrifi

**decentrifi** is a lightweight, end-to-end analytics platform for smart contracts on the Ethereum blockchain and other EVM-compatible chains.

## What It Is

decentrifi provides comprehensive analytics and insights for smart contracts, starting with ERC-20 tokens. The platform consists of:

### Data Pipeline
- **Ingestion Engine**: Captures and processes Transfer events from ERC-20 contracts (initially DAI on Ethereum mainnet)
- **Storage Layer**: Utilizes PostgreSQL with TimescaleDB for efficient time-series data storage
- **Aggregation System**: Automatically computes hourly metrics including:
    - Function call counts by selector
    - Transaction error/revert rates
    - Gas usage distribution
    - Active wallet counts

### Web Application
- Server-side rendered dashboard built with Kotlin and Thymeleaf
- Interactive visualizations using Chart.js, featuring:
    - Calls per function over time
    - Error/revert rate trends
    - Gas usage histograms
    - Daily active wallet metrics
- RESTful API endpoints for all metrics
- Responsive design with date range filtering

### Key Features
- Support for all EVM-compatible chains
- Designed for extensibility with arbitrary ABIs
- Modular, scalable architecture
- Docker-based deployment
- Contract management system to store and manage ABIs and addresses across different chains

## Project Structure

The project is organized as a multi-module Maven application:

- **decentrifi-parent**: The root parent module that defines common dependencies and configurations
- **data-ingestion**: Module responsible for collecting and processing blockchain data
  - Polls for new blocks and extracts events using Ktor and Web3j
  - Supports trace_filter to capture all transactions including internal ones that interact with the contract
  - Stores raw invocations in PostgreSQL with TimescaleDB using Exposed ORM
  - Decodes ERC-20 function calls and tracks transfer events
  - Supports custom batch sizes and polling intervals
  - Maintains state to resume ingestion after restart
  - Includes ABI parsing capabilities to extract functions and events
  - Provides contract management to store and retrieve ABIs and contract addresses
- **analytics-api**: Web application module that provides the dashboard and API
  - Server-side rendered UI with Thymeleaf
  - REST endpoints for accessing metrics
  - Interactive charts using Chart.js

## Technology Stack

- **Backend**: Kotlin, Ktor, Exposed ORM, PostgreSQL, TimescaleDB
- **Blockchain Integration**: Web3j
- **Frontend**: Thymeleaf, HTML/CSS/JS, Chart.js
- **Deployment**: Docker, Docker Compose, Kubernetes (optional)

## How to Build

### Prerequisites
- Java 21 or higher
- Maven 3.8+
- Docker and Docker Compose
- PostgreSQL with TimescaleDB extension
- Git

### Setting Up the Development Environment

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/decentrifi.git
   cd decentrifi
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Set up the database**

   Using Docker Compose:
   ```bash
   cd docker
   docker-compose up -d postgres
   ```

   Or connect to your existing PostgreSQL instance and create the required database:
   ```sql
   CREATE DATABASE decentrifi;
   -- Enable TimescaleDB extension if not already enabled
   CREATE EXTENSION IF NOT EXISTS timescaledb;
   ```

4. **Configure the data ingestion module**

   Edit `data-ingestion/src/main/resources/application.conf`:
   ```hocon
   server {
       port = 8080
   }

   database {
       jdbcUrl = "jdbc:postgresql://localhost:5432/decentrifi"
       username = "postgres"
       password = "postgres"
       maxPoolSize = 10
   }

   ethereum {
       rpcUrl = "https://mainnet.infura.io/v3/YOUR_INFURA_KEY"
       contractAddress = "0x6b175474e89094c44da98b954eedeac495271d0f"
       startBlock = 15000000
       batchSize = 100
       pollingInterval = 15000
   }
   ```

5. **Run the application**

   Using Docker Compose:
   ```bash
   cd docker
   docker-compose up -d
   ```

   Or run each module separately:
   ```bash
   # Run the data ingestion module
   cd data-ingestion
   mvn exec:java -Dexec.mainClass="fi.decentri.dataingest.ApplicationKt"
   ```

### Building for Production

1. **Create production Docker images**
   ```bash
   mvn clean package
   docker build -t decentrifi/data-ingestion:latest -f data-ingestion/Dockerfile data-ingestion
   ```

2. **Deploy with Docker Compose or Kubernetes**
   ```bash
   # Using Docker Compose
   docker-compose -f docker/docker-compose.yml up -d
   ```

## Configuring Blockchain Data Ingestion

The `data-ingestion` module can be configured to ingest data from different ERC-20 contracts by adjusting the following settings:

- **ETH_RPC_URL**: URL of the Ethereum node (or other EVM chain)
- **ETH_CONTRACT_ADDRESS**: Smart contract address to monitor
- **ETH_START_BLOCK**: Starting block number for historical data ingestion
- **ETH_BATCH_SIZE**: Number of blocks to process in one batch
- **ETH_POLLING_INTERVAL**: Interval between checks for new blocks (milliseconds)

### Trace Filter Functionality

The application now utilizes `trace_filter` to capture all transactions (including internal ones) that interact with the target contract. This provides several benefits over the traditional event-based approach:

- Captures all contract interactions, not just those emitting events
- Includes internal transactions (those not directly initiated by EOAs)
- More comprehensive data for analytics purposes
- Ability to analyze failed transactions and their gas usage

Note that to use the `trace_filter` functionality, your Ethereum node must support this API method. It is supported by:

- Erigon (formerly Turbo-Geth)
- OpenEthereum (formerly Parity)
- Geth with debug API enabled (--rpcapi "debug")
- Infura (requires appropriate plan)
- Alchemy (requires appropriate plan)

By default, the application now uses `trace_filter` for data ingestion. The system maintains separate metadata for trace-based ingestion, allowing you to restart historical ingestion without conflicts.

Example for ingesting data from USDC instead of DAI:
```
ETH_CONTRACT_ADDRESS=0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48
ETH_START_BLOCK=10000000
```

## ABI Processing

The platform includes functionality to parse smart contract ABI files, which is essential for working with different types of contracts and understanding their interfaces.

### Using the ABI Service

The `AbiService` class provides methods for working with ABIs:

```kotlin
// Create an instance of the ABI service
val abiService = AbiService()

// Parse an ABI JSON string
val (functions, events) = abiService.parseABI(abiJsonString)

// Now you can work with the parsed functions and events
functions.forEach { function ->
    println("Function: ${function.name}")
    println("  Inputs: ${function.inputs.size}")
    println("  Outputs: ${function.outputs.size}")
    println("  StateMutability: ${function.stateMutability}")
}

events.forEach { event ->
    println("Event: ${event.name}")
    println("  Inputs: ${event.inputs.size}")
    println("  Anonymous: ${event.anonymous}")
}
```

The `parseABI` method returns a `Pair<List<AbiFunction>, List<AbiEvent>>` with the following structure:

- **AbiFunction** properties:
  - `name`: Name of the function
  - `inputs`: List of function input parameters
  - `outputs`: List of function output parameters
  - `stateMutability`: The state mutability (view, pure, payable, etc.)
  - `constant`: Whether the function is constant
  - `payable`: Whether the function accepts ETH

- **AbiEvent** properties:
  - `name`: Name of the event
  - `inputs`: List of event parameters
  - `anonymous`: Whether the event is anonymous

Both function and event inputs are represented by **AbiFunctionParameter** objects with these properties:
  - `name`: Name of the parameter
  - `type`: Solidity type (e.g., "uint256", "address", etc.)
  - `indexed`: Whether the parameter is indexed (relevant for events)

## Contract Management

The platform includes a contract management system to store and retrieve ABIs and contract addresses. This feature allows the application to work with multiple contracts across different chains.

### Contract Model

The `Contract` entity includes the following properties:
- `id`: Unique identifier for the contract record
- `address`: The contract address (e.g., '0x6B17…')
- `abi`: The contract ABI as a JSON string
- `chain`: The blockchain network (e.g., 'ethereum-mainnet', 'bsc-mainnet')
- `name`: Optional name for the contract
- `createdAt`: When the record was created
- `updatedAt`: When the record was last updated

### Using the Contracts Service

The `ContractsService` provides methods for managing contracts:

```kotlin
// Create a new contract
val contractsService = ContractsService(contractsRepository, abiService)
val contract = contractsService.createContract(
    address = "0x6b175474e89094c44da98b954eedeac495271d0f",
    abi = abiJsonString,
    chain = "ethereum-mainnet",
    name = "DAI Stablecoin"
)

// Find a contract by address and chain
val daiContract = contractsService.findContractByAddressAndChain(
    address = "0x6b175474e89094c44da98b954eedeac495271d0f", 
    chain = "ethereum-mainnet"
)

// Use the ABI with the ABI service
if (daiContract != null) {
    val (functions, events) = abiService.parseABI(daiContract.abi)
    // Work with the parsed functions and events
}

// Get all contracts on a specific chain
val ethereumContracts = contractsService.findContractsByChain("ethereum-mainnet")

// Delete a contract
contractsService.deleteContract(contractId)
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.