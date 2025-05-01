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

## Project Structure

The project is organized as a multi-module Maven application:

- **decentrifi-parent**: The root parent module that defines common dependencies and configurations
- **data-ingestion**: Module responsible for collecting and processing blockchain data
  - Polls for new blocks and extracts events using Ktor and Web3j
  - Stores raw invocations in PostgreSQL with TimescaleDB using Exposed ORM
  - Decodes ERC-20 function calls and tracks transfer events
  - Supports custom batch sizes and polling intervals
  - Maintains state to resume ingestion after restart
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

Example for ingesting data from USDC instead of DAI:
```
ETH_CONTRACT_ADDRESS=0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48
ETH_START_BLOCK=10000000
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.