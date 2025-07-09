# Order Sourcing Engine

## Overview

A high-performance Spring Boot application that optimizes order fulfillment by intelligently selecting the best locations to fulfill customer orders. Achieves **sub-50ms response times** through advanced caching, configurable scoring systems, and intelligent batch processing.

## Key Features

- **Sub-50ms Performance**: Optimized for high-speed order processing
- **Configurable Scoring**: Multiple scoring configurations for different business scenarios
- **Intelligent Processing**: Automatic batch vs sequential processing decisions
- **Simplified API**: Clean, minimal response format for frontend integration
- **Multi-Location Support**: Handles both single and multi-location fulfillment strategies

## Quick Start

### Running with H2 Database (Recommended for testing)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

### Running with PostgreSQL
1. Set up PostgreSQL database named `order_sourcing`
2. Update credentials in `application.yml`
3. Run: `mvn spring-boot:run`

## API Endpoints

### Main Endpoints
- `POST /api/sourcing/source-simplified` - Returns simplified fulfillment information
- `POST /api/sourcing/source-realtime` - Returns detailed fulfillment information
- `GET /api/sourcing/source-realtime/health` - Health check
- `POST /api/sourcing/source-realtime/validate` - Validate order structure

### Configuration Endpoints
- `GET /api/sourcing/scoring-configurations` - Get all scoring configurations
- `GET /api/sourcing/scoring-configurations/{configId}` - Get specific configuration

## Sample API Request

### Simplified Sourcing Request
```bash
curl -X POST http://localhost:8080/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "ORDER_001",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "unitPrice": 299.99
      }
    ],
    "customerId": "CUST_001",
    "orderType": "WEB"
  }'
```

### Response Format
```json
{
  "orderId": "ORDER_001",
  "processingTimeMs": 35,
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

## Technology Stack

- **Framework**: Spring Boot 3.1.0
- **Language**: Java 17
- **Database**: H2 (in-memory) / PostgreSQL
- **Rule Engine**: AviatorScript 5.3.3
- **Caching**: Spring Cache
- **Build Tool**: Maven

## Documentation

### 📚 Available Guides

1. **[PROJECT_ARCHITECTURE_AND_FLOWS.md](PROJECT_ARCHITECTURE_AND_FLOWS.md)** - Complete technical architecture, data flows, and system design
2. **[BUSINESS_CONFIGURATION_GUIDE.md](BUSINESS_CONFIGURATION_GUIDE.md)** - Business guide for customizing order routing with scoring configurations
3. **[API_EXAMPLES_AND_SCENARIOS.md](API_EXAMPLES_AND_SCENARIOS.md)** - Comprehensive API examples for various business scenarios
4. **[SIMPLIFIED_API_EXAMPLE.md](SIMPLIFIED_API_EXAMPLE.md)** - Quick start guide for the simplified API endpoint

### 🔧 Configuration Options

The system supports multiple scoring configurations:
- **Default Scoring**: Standard fulfillment weights
- **Electronics Premium**: High-value electronics with security requirements
- **Express Delivery**: Optimized for same-day/next-day delivery
- **Hazmat**: Specialized handling for hazardous materials
- **Peak Season**: Adjusted for high-demand periods

### 🚀 Performance Expectations

| Scenario | Response Time | Strategy |
|----------|---------------|----------|
| Single Item (PDP) | 15-40ms | Sequential |
| Mixed Cart (2-5 items) | 40-80ms | Batch |
| B2B Bulk (5+ items) | 80-150ms | Batch |
| Performance Test (10+ items) | 150-300ms | Batch |

### 🛠️ Database Setup

The application automatically creates tables and populates sample data on startup when using H2. For production PostgreSQL setup:

```sql
-- Sample locations and inventory are automatically created
-- Customize scoring configurations as needed
-- See BUSINESS_CONFIGURATION_GUIDE.md for details
```

### 💡 Key Features

- **Intelligent Processing**: Automatic batch vs sequential processing decisions
- **Configurable Scoring**: Dynamic location selection based on business rules
- **Multi-Location Support**: Handles both single and multi-location fulfillment
- **Simplified Responses**: Clean, minimal JSON for frontend integration
- **Performance Optimized**: Sub-50ms response times with caching and parallel processing

### 🔍 Monitoring

- Health check endpoint: `GET /api/sourcing/source-realtime/health`
- Validation endpoint: `POST /api/sourcing/source-realtime/validate`
- Configuration endpoints: `GET /api/sourcing/scoring-configurations`

For detailed implementation examples, business configuration options, and API usage patterns, please refer to the comprehensive documentation files listed above.