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

### Web Application
- REST API serving analytics data
- Interactive visualizations featuring:
    - Gas usage metrics
    - Daily active wallet metrics
- RESTful API endpoints for all metrics
- Responsive design with date range filtering

### Key Features
- Support for EVM-compatible chains
- Designed for extensibility with arbitrary ABIs
- Modular, scalable architecture
- Kubernetes-based deployment
- Contract management system to store and manage ABIs and addresses across different chains
- Waitlist functionality for early access users

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
- **db**: Shared database module containing connection logic and models

## Technology Stack

- **Backend**: Kotlin, Ktor, Exposed ORM, PostgreSQL, TimescaleDB
- **Blockchain Integration**: Web3j
- **Frontend**: Thymeleaf, HTML/CSS/JS
- **Deployment**: Docker, Kubernetes
- **Functional Programming**: Arrow
- **Dependency Injection**: Koin
- **Connection Pooling**: HikariCP

## How to Build

### Prerequisites
- Java 21
- Maven 3.8+
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
   docker build -t decentrifi/data-api:latest -f data-api/Dockerfile data-api
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

The data ingestion module supports multiple operation modes:

1. **Auto Mode (Default)**: Automatically processes all contracts registered in the system
   ```bash
   # Run in auto mode (default if no mode specified)
   java -jar data-ingestion.jar --mode=auto
   ```

2. **Contract Mode**: Processes a specific contract on a specific network
   ```bash
   # Process a single contract on a specific network
   java -jar data-ingestion.jar --mode=contract --contract 0x1234abcd... --network ethereum-mainnet
   ```

3. **Kubernetes Job Launcher**: The data-api module provides a RESTful interface to launch contract-specific ingestion jobs on Kubernetes
   - Triggers Kubernetes jobs with contract mode parameters
   - Useful for on-demand processing of specific contracts
   - Automatically triggered when a new contract is submitted through the web interface

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

## Waitlist Functionality

The project includes a waitlist system for early access signups. The waitlist allows potential users to register their interest in the platform before general availability.

## Kubernetes Deployment

The project includes Terraform configuration to deploy the application to Kubernetes. The configuration can be found in the `infra` directory.

The deployment includes:
- Deployments for each microservice (data-ingestion, data-api)
- Services to expose the microservices
- ConfigMaps and Secrets for configuration
- Integration with Cloudflare for DNS and TLS

To deploy the application to Kubernetes:
```bash
cd infra
terraform init
terraform apply
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.