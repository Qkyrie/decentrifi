<p align="center">
  <img src="../logos/logo_full.png" alt="Decentrifi Logo" width="400"/>
</p>

# decentrifi

[![Build Status](https://github.com/Qkyrie/decentrifi/actions/workflows/build-main.yml/badge.svg)](https://github.com/Qkyrie/decentrifi/actions/workflows/build-main.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**decentrifi** is a lightweight, end-to-end analytics platform for smart contracts on the Ethereum blockchain and other EVM-compatible chains.

## What It Is

decentrifi provides comprehensive analytics and insights for smart contracts, starting with ERC-20 tokens. The platform consists of:

### Data Pipeline
- **Ingestion Engine**: Captures and processes blockchain data including function calls and transaction details
- **Storage Layer**: Utilizes PostgreSQL with TimescaleDB for efficient time-series data storage
- **Aggregation System**: Computes metrics including:
    - Function call counts by selector
    - Gas usage distribution
    - Active wallet counts
- **Data Aggregator**: New Go-based service for efficient data aggregation and processing

### Web Application
- REST API serving analytics data
- Interactive visualizations featuring:
    - Gas usage metrics
    - Daily active wallet metrics
    - Live user count with auto-refresh (users active in the last 30 minutes)
- RESTful API endpoints for all metrics
- Responsive design with date range filtering
- Modularized routing logic for better maintainability

### Key Features
- Support for EVM-compatible chains
- Designed for extensibility with arbitrary ABIs
- Modular, scalable architecture
- Kubernetes-based deployment
- Contract management system to store and manage ABIs and addresses across different chains
- Waitlist functionality for early access users
- Explicit network configuration (no default fallbacks)

## Project Structure

The project is organized as a multi-module Maven application:

- **decentrifi-parent**: The root parent module that defines common dependencies and configurations
- **data-ingestion**: Module responsible for collecting and processing blockchain data
  - Polls for new blocks and extracts events using Ktor and Web3j
  - Supports trace_filter to capture all transactions including internal ones that interact with the contract
  - Stores raw invocations in PostgreSQL with TimescaleDB using Exposed ORM
  - Decodes function calls
  - Supports custom batch sizes and polling intervals
  - Maintains state to resume ingestion after restart
  - Includes ABI parsing capabilities to extract functions and events
  - Provides contract management to store and retrieve ABIs and contract addresses
- **data-api**: Web application module that provides the dashboard and API
  - REST endpoints for accessing metrics
  - Interactive charts
  - Analytics dashboard for contract metrics
  - Kubernetes job launcher for manual data ingestion of specific contracts
  - Modular route structure for better code organization
- **db**: Shared database module containing connection logic and models
- **data-aggregator**: New Go-based service for efficient data processing and analytics aggregation

## Technology Stack

- **Backend**: 
  - Kotlin, Ktor, Exposed ORM, PostgreSQL, TimescaleDB
  - Go for the data-aggregator service
- **Blockchain Integration**: Web3j with multi-network support
- **Command-line Parsing**: Clikt for robust CLI argument handling
- **Frontend**: Thymeleaf, HTML/CSS/JS
- **Deployment**: Docker, Kubernetes, GitHub Container Registry
- **Functional Programming**: Arrow
- **Dependency Injection**: Koin
- **Connection Pooling**: HikariCP
- **CI/CD**: GitHub Actions workflows for both Kotlin and Go services

## How to Build

### Prerequisites
- Java 21
- Maven 3.8+
- Go 1.21+ (for data-aggregator service)
- Docker and Docker Compose
- PostgreSQL with TimescaleDB extension
- Git

### Setting Up the Development Environment

1. **Clone the repository**
   ```bash
   git clone https://github.com/Qkyrie/decentrifi.git
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

   Edit `data-ingestion/src/main/resources/application.conf` with appropriate database and blockchain connection settings.

   Example multi-network configuration:
   ```hocon
   networks {
     ethereum-mainnet {
       rpcUrl = "https://mainnet.infura.io/v3/your-api-key"
       batchSize = 1000
       pollingInterval = 15000
       blockTime = 12
     }
     polygon-mainnet {
       rpcUrl = "https://polygon-rpc.com"
       batchSize = 2000
       pollingInterval = 5000
       blockTime = 2
     }
   }
   ```

   Note: As of recent updates, explicit network configuration is required - there are no default fallbacks to Ethereum.

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
   
   # Run the data-aggregator service (Go)
   cd services/data-aggregator
   go run main.go
   ```

### Building for Production

1. **Create production Docker images**
   ```bash
   # For Kotlin services
   mvn clean package
   docker build -t decentrifi/data-ingestion:latest -f data-ingestion/Dockerfile data-ingestion
   docker build -t decentrifi/data-api:latest -f data-api/Dockerfile data-api
   
   # For Go data-aggregator service
   cd services/data-aggregator
   docker build -t decentrifi/data-aggregator:latest .
   ```

2. **Deploy with Kubernetes**

   The project includes Terraform configurations for Kubernetes deployment:
   ```bash
   cd infra
   terraform init
   terraform apply
   ```

## Configuring Blockchain Data Ingestion

The `data-ingestion` module can be configured to ingest data from different blockchain contracts by adjusting settings in the application.conf file.

### Ingestion Modes

The data ingestion module supports multiple operation modes with enhanced command-line argument parsing using Clikt:

1. **Auto Mode (Default)**: Automatically processes all contracts registered in the system
   ```bash
   # Run in auto mode (default if no mode specified)
   java -jar data-ingestion.jar --mode=auto
   ```
   - Features a 30-minute cooldown mechanism to prevent redundant processing of recently ingested contracts
   - Intelligently skips contracts that have been manually processed recently

2. **Contract Mode**: Processes a specific contract on a specific network
   ```bash
   # Process a single contract on a specific network
   java -jar data-ingestion.jar --mode=contract --contract 0x1234abcd... --network ethereum-mainnet
   ```
   - Allows targeted data ingestion for specific contracts
   - Updates timestamp metadata to coordinate with auto mode cooldown

3. **Kubernetes Job Launcher**: The data-api module provides a RESTful interface to launch contract-specific ingestion jobs on Kubernetes
   - Triggers Kubernetes jobs with contract mode parameters
   - Useful for on-demand processing of specific contracts
   - Automatically triggered when a new contract is submitted through the web interface
   - Creates jobs with proper service account and resource configurations

### Trace Filter Functionality

The application utilizes `trace_filter` to capture all transactions (including internal ones) that interact with the target contract. This provides several benefits over the traditional event-based approach:

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

## ABI Processing

The platform includes functionality to parse smart contract ABI files, which is essential for working with different types of contracts and understanding their interfaces. ABI-related classes are now organized in the `infrastructure.abi` package for better code structure.

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

## Contract Management

The platform includes a contract management system to store and retrieve ABIs and contract addresses. This feature allows the application to work with multiple contracts across different chains.

### Contract Model

The `Contract` entity includes the following properties:
- `id`: Unique identifier for the contract record
- `address`: The contract address (e.g., '0x6B17…')
- `abi`: The contract ABI as a JSON string
- `chain`: The blockchain network (e.g., 'ethereum-mainnet', 'polygon-mainnet')
- `name`: Optional name for the contract
- `createdAt`: When the record was created
- `updatedAt`: When the record was last updated

### Multi-Network Support

The platform now includes robust support for multiple blockchain networks through the `Web3jManager`:

- **Dynamic Network Configuration**: Configure multiple networks in application.conf
- **Connection Management**: Efficient management of Web3j instances with connection pooling
- **Lazy Initialization**: Web3j connections are created on-demand
- **Network-Specific Settings**: Configure each network with custom batch sizes, polling intervals, and block times
- **Resource Management**: Automatic cleanup of connections when shutting down
- **Explicit Network Selection**: No default fallbacks to Ethereum - network must be explicitly specified

Example usage:
```kotlin
// Access the Web3jManager
val web3jManager = Web3jManager.getInstance()

// Get a Web3j instance for a specific network
val web3j = web3jManager.web3("ethereum-mainnet")

// Get network-specific configuration
val networkConfig = web3jManager.getNetworkConfig("polygon-mainnet")
```

## Web Application Features

### Route Structure

The web application uses a modular routing approach with separate route modules:
- `BaseRoutes`: Core routes and common functionality
- `ContractRoutes`: Contract management endpoints
- `WaitlistRoutes`: Waitlist registration and management
- `AnalyticsRoutes`: Data analytics endpoints
- Centralized `configureRoutesModules` function integrates all route modules

### Live User Count Tracking

The application includes functionality to display and auto-refresh the count of users active in the last 30 minutes:
- Dedicated API endpoint for fetching live user data
- Front-end integration with animated updates every 5 seconds
- Real-time visibility into platform activity

## Waitlist Functionality

The project includes a waitlist system for early access signups. The waitlist allows potential users to register their interest in the platform before general availability.

## Kubernetes Deployment

The project includes Terraform configuration to deploy the application to Kubernetes. The configuration can be found in the `infra` directory.

The deployment includes:
- Deployments for each microservice (data-ingestion, data-api, data-aggregator)
- Services to expose the microservices
- ConfigMaps and Secrets for configuration
- Integration with Cloudflare for DNS and TLS

To deploy the application to Kubernetes:
```bash
cd infra
terraform init
terraform apply
```

## CI/CD Pipelines

The project uses GitHub Actions for continuous integration and deployment:
- Separate workflows for Kotlin and Go services
- Automatic builds and tests on relevant changes
- Docker image creation and publishing to GitHub Container Registry
- Environment variables for registry and image name configuration

## License

This project is licensed under the MIT License - see the LICENSE file for details.