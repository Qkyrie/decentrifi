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
  - Polls for new blocks and extracts events
  - Stores raw invocations in PostgreSQL with TimescaleDB
  - Aggregates metrics on an hourly basis
- **analytics-api**: Web application module that provides the dashboard and API
  - Server-side rendered UI with Thymeleaf
  - REST endpoints for accessing metrics
  - Interactive charts using Chart.js

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
   docker-compose up -d postgres
   ```

   Or connect to your existing PostgreSQL instance and create the required database:
   ```sql
   CREATE DATABASE decentrifi;
   -- Enable TimescaleDB extension if not already enabled
   CREATE EXTENSION IF NOT EXISTS timescaledb;
   ```

4. **Configure application properties**

   Create `data-ingestion/src/main/resources/application.properties` with the following:
   ```properties
   # Database Configuration
   spring.datasource.url=jdbc:postgresql://localhost:5432/decentrifi
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   spring.jpa.hibernate.ddl-auto=validate
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
   
   # Ethereum Configuration
   ethereum.rpc.url=https://mainnet.infura.io/v3/YOUR_INFURA_PROJECT_ID
   ethereum.contract.dai=0x6b175474e89094c44da98b954eedeac495271d0f
   
   # Application Configuration
   ingestion.batch-size=100
   ingestion.poll-interval=15000
   ```

   Create `analytics-api/src/main/resources/application.properties` with the following:
   ```properties
   # Server Configuration
   server.port=8080
   
   # Database Configuration
   spring.datasource.url=jdbc:postgresql://localhost:5432/decentrifi
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   spring.jpa.hibernate.ddl-auto=validate
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
   
   # Application Configuration
   metrics.cache-ttl=300
   ```

5. **Run database migrations**
   ```bash
   cd data-ingestion
   ./create-migration.sh "Initial schema setup"
   mvn spring-boot:run
   ```

6. **Run the application modules**
   ```bash
   # Run the data ingestion module
   mvn spring-boot:run -pl data-ingestion
   
   # Run the analytics API module in a separate terminal
   mvn spring-boot:run -pl analytics-api
   ```

### Building for Production

1. **Create production Docker images**
   ```bash
   mvn clean package
   docker build -t decentrifi/ingestion:latest -f data-ingestion/Dockerfile data-ingestion
   docker build -t decentrifi/analytics-api:latest -f analytics-api/Dockerfile analytics-api
   ```

2. **Deploy with Kubernetes**
   ```bash
   # Apply the Helm chart
   helm install decentrifi ./helm/decentrifi
   ```

## License

This project is licensed under the MIT License - see the LICENSE file for details.