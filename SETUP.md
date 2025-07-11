# Order Sourcing Engine - Local Setup Guide

## Overview

The Order Sourcing Engine is a high-performance Spring Boot application that optimizes order fulfillment by intelligently selecting the best locations to fulfill customer orders. It achieves sub-50ms response times through advanced caching, configurable scoring systems, and intelligent batch processing.

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **PostgreSQL 12+** (or H2 for testing)
- **Git**

## Quick Start

### Option 1: Using H2 In-Memory Database (Recommended for Testing)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd order-sourcing-engine
   ```

2. **Update application-test.yml for H2**
   ```yaml
   # src/main/resources/application-test.yml
   server:
     port: 8081
   spring:
     datasource:
       url: jdbc:h2:mem:testdb
       driver-class-name: org.h2.Driver
       username: sa
       password: 
     h2:
       console:
         enabled: true
     jpa:
       hibernate:
         ddl-auto: create-drop
       show-sql: false
       database-platform: org.hibernate.dialect.H2Dialect
       defer-datasource-initialization: true
     sql:
       init:
         mode: always
         data-locations: classpath:data-test.sql
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=test
   ```

4. **Verify it's running**
   ```bash
   curl http://localhost:8081/actuator/health
   ```

### Option 2: Using PostgreSQL (Production Setup)

1. **Install and setup PostgreSQL**
   ```bash
   # Ubuntu/Debian
   sudo apt update
   sudo apt install postgresql postgresql-contrib

   # macOS
   brew install postgresql
   brew services start postgresql

   # Windows
   # Download and install from https://www.postgresql.org/download/windows/
   ```

2. **Create database and user**
   ```sql
   sudo -u postgres psql
   CREATE DATABASE order_sourcing;
   CREATE USER demouser WITH ENCRYPTED PASSWORD '123456';
   GRANT ALL PRIVILEGES ON DATABASE order_sourcing TO demouser;
   \q
   ```

3. **Update application.yml**
   ```yaml
   # src/main/resources/application.yml
   server:
     port: 8081
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/order_sourcing
       driverClassName: org.postgresql.Driver
       username: demouser
       password: 123456
     jpa:
       hibernate:
         ddl-auto: update
       show-sql: true
       database-platform: org.hibernate.dialect.PostgreSQLDialect
       defer-datasource-initialization: true
     sql:
       init:
         mode: always
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

## Testing the Setup

### 1. Health Check
```bash
curl http://localhost:8081/actuator/health
```

### 2. Basic Order Sourcing Test
```bash
curl -X POST http://localhost:8081/api/sourcing/source \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "TEST_001",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE"
      }
    ],
    "customerId": "CUST_001",
    "orderType": "WEB"
  }'
```

### 3. Expected Response Format
```json
{
  "orderId": "TEST_001",
  "processingTimeMs": 45,
  "fulfillmentPlans": [
    {
      "sku": "PHONE123",
      "requestedQuantity": 1,
      "totalFulfilled": 1,
      "isPartialFulfillment": false,
      "overallScore": 85.5,
      "locationAllocations": [
        {
          "locationId": 1,
          "locationName": "Downtown Warehouse",
          "allocatedQuantity": 1,
          "locationScore": 85.5,
          "deliveryTiming": {
            "estimatedShipDate": "2024-01-15T09:00:00",
            "estimatedDeliveryDate": "2024-01-18T17:00:00",
            "transitTimeDays": 3,
            "processingTimeHours": 24
          }
        }
      ]
    }
  ]
}
```

## Development Tools

### H2 Console (when using H2)
- URL: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

### PostgreSQL Admin Tools
- **pgAdmin**: Web-based PostgreSQL administration
- **DBeaver**: Universal database tool
- **Command Line**: `psql -h localhost -U demouser -d order_sourcing`

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BatchSourcingServiceTest

# Run tests with coverage
mvn clean verify
```

## Building for Production

```bash
# Create executable JAR
mvn clean package

# Run the JAR
java -jar target/order-sourcing-engine-1.0.0.jar --spring.profiles.active=prod
```

## Performance Expectations

| Scenario | Response Time | Strategy |
|----------|---------------|----------|
| Single Item (PDP) | 15-40ms | Sequential |
| Mixed Cart (2-5 items) | 40-80ms | Batch |
| B2B Bulk (5+ items) | 80-150ms | Batch |
| Performance Test (10+ items) | 150-300ms | Batch |

## Troubleshooting

### Common Issues

1. **Port 8081 already in use**
   ```bash
   # Find process using port 8081
   lsof -i :8081
   # Kill the process
   kill -9 <PID>
   ```

2. **Database connection failed**
   - Verify PostgreSQL is running: `sudo systemctl status postgresql`
   - Check database exists: `psql -l`
   - Verify credentials in application.yml

3. **Out of memory errors**
   ```bash
   export MAVEN_OPTS="-Xmx2g"
   mvn spring-boot:run
   ```

4. **Tests failing**
   ```bash
   # Run with H2 for testing
   mvn test -Dspring.profiles.active=test
   ```

### Logs Location

- **Application logs**: Console output or configured log file
- **Spring Boot logs**: Check `logging.level` in application.yml
- **Database logs**: PostgreSQL logs in `/var/log/postgresql/`

## Next Steps

- See [IMPLEMENTATION.md](IMPLEMENTATION.md) for business configuration and AviatorScript details
- See [API_EXAMPLES.md](API_EXAMPLES.md) for comprehensive API usage examples
- Configure scoring rules and location filters for your business needs