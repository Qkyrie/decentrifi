# Chainlayer - Blockchain Interaction API

Chainlayer is a comprehensive platform that provides a REST API for interacting with various blockchain networks, focusing primarily on EVM-compatible chains. The application is built with Kotlin and Spring Boot, offering a secure and efficient way to interact with blockchain data.

## Overview

Chainlayer serves as a proxy for blockchain interactions, abstracting away the complexities of direct blockchain communication. It provides REST endpoints for common blockchain operations such as:

- Reading contract state
- Getting native token balances
- Retrieving event logs
- Decoding transaction data
- Finding proxy contract implementations

Currently, the platform supports BASE chain (Chain ID: 8453) with the possibility of extending to other EVM-compatible chains in the future.

## Features

- **Contract State Reading**: Query smart contract state without writing any blockchain code
- **Native Balance Checking**: Get native token balances for any address
- **Event Log Retrieval**: Fetch and filter blockchain events
- **Tuple Decoding**: Decode complex data structures returned from smart contracts
- **Proxy Contract Resolution**: Automatically discover implementation addresses for proxy contracts

## Tech Stack

- **Language**: Kotlin 2.1
- **Framework**: Spring Boot 3.3
- **JVM Version**: Java 21
- **Blockchain Interaction**: Web3j 4.13.0
- **HTTP Client**: Ktor 2.3.13
- **JSON Processing**: Jackson, GSON
- **Functional Programming**: Arrow-kt 1.2.4
- **Concurrency**: Kotlin Coroutines 1.9.0
- **Caching**: Cache4k 0.13.0

## API Endpoints

### Contract Interaction

- `POST /{chain}/contract/call` - Make a raw contract call
- `POST /{chain}/contract/read` - Read contract state using a simplified interface
- `GET /{chain}/contract/{contract}/find-proxy` - Find implementation address for a proxy contract
- `POST /{chain}/contract/build-event-topic` - Create an event topic signature for filtering events

### Balance Checking

- `GET /{chain}/balance/{address}` - Get native token balance for an address

### Event Logs

- `POST /{chain}/events/logs` - Get filtered event logs from the blockchain

### Tuple Decoding

- `POST /tuple-decoder` - Decode ABI-encoded tuple data

## Configuration

The application is configured via Spring Boot properties:

- `application.properties` - Main configuration
- `application-dev.properties` - Development-specific settings

Key configuration parameters:
- `org.cipheredge.base.endpoint.url` - BASE chain RPC endpoint URL
- `server.port` - HTTP server port (default: 8081)

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

### Building the Project

```bash
mvn clean package
```

### Running the Application

```bash
java -jar target/chainlayer-0.0.1-SNAPSHOT.jar
```

Or using Maven:

```bash
mvn spring-boot:run
```

### Development Mode

To run in development mode with dev-specific properties:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Example Usage

### Reading Contract State

```bash
curl -X POST http://localhost:8081/base/contract/read \
  -H "Content-Type: application/json" \
  -d '{
    "to": "0xYourContractAddress",
    "methodName": "balanceOf",
    "inputs": [
      {
        "type": "address",
        "value": "0xUserAddress"
      }
    ],
    "outputs": [
      {
        "type": "uint256"
      }
    ]
  }'
```

### Checking Native Balance

```bash
curl http://localhost:8081/base/balance/0xYourAddress
```

## Project Structure

The application follows a standard Spring Boot structure with clear separation of concerns:

- `org.cipheredge.chain` - Core blockchain interaction logic
- `org.cipheredge.chain.evm` - EVM-specific blockchain code
- `org.cipheredge.rest` - REST controllers and API definition
- `org.cipheredge.rest.request` - Request models and validation
- `org.cipheredge.web3j` - Web3j extensions and utilities

## Extending to New Chains

To add support for additional blockchain networks:

1. Add the new chain to the `Chain` enum in `org.cipheredge.chain.Chain.kt`
2. Configure RPC endpoints in application properties
3. Update the `Web3jConfig` class to include the new chain

## License

MIT License

Copyright (c) 2025 CipherEdge

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.