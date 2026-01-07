# Kafka Calculator

A distributed calculator application built with **Java 17** and **Apache Kafka**. Calculation requests are sent as messages through Kafka, processed by a service, and results are returned via Kafka topics.

## Architecture

```
┌─────────────────┐     calculator-requests       ┌────────────────────┐
│ Calculator      │ ───────────────────────────▶ │ Calculator         │
│ Client          │                               │ Service            │
│ (Producer)      │ ◀─────────────────────────── │ (Consumer)         │
└─────────────────┘     calculator-results        └────────────────────┘
```

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **Docker** and **Docker Compose**

## Quick Start

### 1. Start Kafka

```bash
docker-compose up -d
```

Wait ~30 seconds for Kafka to be ready.

### 2. Build the Project

```bash
./mvnw clean package -DskipTests
```

### 3. Start the Calculator Service

In one terminal:

```bash
java -jar target/kafka-calculator-1.0.0.jar --service
```

### 4. Start the Calculator Client

In another terminal:

```bash
java -jar target/kafka-calculator-1.0.0.jar --client
```

### 5. Perform Calculations

```
calc> 5 + 3
= 8
calc> 10 * 4
= 40
calc> 100 / 5
= 20
calc> 50 - 25
= 25
calc> exit
```

## Supported Operations

| Operator | Operation       |
|----------|-----------------|
| `+`      | Addition        |
| `-`      | Subtraction     |
| `*`      | Multiplication  |
| `/`      | Division        |

## Project Structure

```
├── pom.xml                  # Maven configuration
├── docker-compose.yml       # Kafka + Zookeeper setup
└── src/main/java/com/calculator/
    ├── model/
    │   ├── CalculationRequest.java
    │   └── CalculationResult.java
    ├── config/
    │   └── KafkaConfig.java
    ├── service/
    │   ├── Calculator.java
    │   └── CalculatorService.java
    ├── client/
    │   └── CalculatorClient.java
    └── CalculatorApplication.java
```

## Cleanup

```bash
docker-compose down
```