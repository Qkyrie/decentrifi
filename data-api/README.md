# Decentrifi Data API

This module provides API access to the analytics data collected by the Decentrifi system.

## Features

- REST API for querying contract and transaction data
- Built with Ktor and Kotlin Coroutines
- Exposed ORM for database access
- Supports pagination and filtering of results

## Development Setup

### Prerequisites

- JDK 21
- Maven
- PostgreSQL database

### Building

```bash
mvn clean package
```

### Running Locally

```bash
java -jar target/app.jar
```

Or using Maven:

```bash
mvn exec:java -Dexec.mainClass="fi.decentri.dataapi.ApplicationKt"
```

### Configuration

Configure the application through environment variables or by modifying `application.conf`:

- `SERVER_PORT`: API server port (default: 8081)
- `DB_JDBC_URL`: Database connection URL
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `DB_MAX_POOL_SIZE`: Connection pool size (default: 10)

### API Endpoints

- `GET /health`: Health check endpoint
- `GET /api/v1/contracts`: List contracts (to be implemented)
- `GET /api/v1/transactions`: List transactions (to be implemented)

## Docker

Build the Docker image:

```bash
docker build -t decentrifi/data-api .
```

Run the container:

```bash
docker run -p 8081:8081 -e DB_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/decentrifi decentrifi/data-api
```