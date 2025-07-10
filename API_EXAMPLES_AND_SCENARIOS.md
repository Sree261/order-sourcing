# API Examples and Scenarios - Order Sourcing Engine

## Overview

This document provides comprehensive API examples for the Order Sourcing Engine, demonstrating various business scenarios and use cases with both the detailed and simplified response formats.

## 🚀 Quick Start

### Base URL
```
http://localhost:8081/api/sourcing
```

### Available Endpoints
- `POST /source-simplified` - Returns simplified fulfillment information
- `POST /source-realtime` - Returns detailed fulfillment information
- `GET /source-realtime/health` - Health check
- `POST /source-realtime/validate` - Validate order structure

## 📱 Common Business Scenarios

### Scenario 1: E-commerce Single Item (PDP)
**Use Case**: Product detail page availability check for standard delivery

```bash
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "PDP_SINGLE_001",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "productCategory": "ELECTRONICS_MOBILE"
      }
    ],
    "customerId": "CUST_001",
    "customerTier": "STANDARD",
    "orderType": "WEB"
  }'
```

**Expected Response**:
```json
{
  "orderId": "PDP_SINGLE_001",
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

### Scenario 2: Mixed Shopping Cart
**Use Case**: Customer cart with different delivery requirements

```bash
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "MIXED_CART_001",
    "latitude": 40.7489,
    "longitude": -73.9857,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 2,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "SDD_FILTER_RULE",
        "scoringConfigurationId": "EXPRESS_DELIVERY_SCORING",
        "productCategory": "ELECTRONICS_MOBILE",
        "isExpressPriority": true
      },
      {
        "sku": "LAPTOP456",
        "quantity": 1,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "scoringConfigurationId": "ELECTRONICS_PREMIUM_SCORING",
        "productCategory": "ELECTRONICS_COMPUTER",
        "specialHandling": "HIGH_VALUE",
        "requiresSignature": true
      },
      {
        "sku": "HEADPHONES101",
        "quantity": 3,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "productCategory": "ELECTRONICS_AUDIO",
        "allowPartialFulfillment": true
      }
    ],
    "customerId": "CUST_002",
    "customerTier": "PREMIUM",
    "allowPartialShipments": true,
    "orderType": "WEB"
  }'
```

### Scenario 3: B2B Bulk Order
**Use Case**: Enterprise customer placing large quantity order

```bash
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "B2B_BULK_001",
    "latitude": 39.9526,
    "longitude": -75.1652,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 100,
        "deliveryType": "STANDARD",
        "locationFilterId": "PEAK_SEASON_RULE",
        "scoringConfigurationId": "B2B_BULK_SCORING",
        ,
        "productCategory": "ELECTRONICS_MOBILE",
        "allowPartialFulfillment": true,
        "preferSingleLocation": false
      },
      {
        "sku": "LAPTOP456",
        "quantity": 50,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "scoringConfigurationId": "B2B_BULK_SCORING",
        ,
        "productCategory": "ELECTRONICS_COMPUTER",
        "requireFullQuantity": true,
        "specialHandling": "CORPORATE_DELIVERY"
      }
    ],
    "customerId": "CORP_001",
    "customerTier": "ENTERPRISE",
    "allowPartialShipments": true,
    "preferSingleLocation": false,
    "orderType": "B2B",
    "salesChannel": "B2B_PORTAL"
  }'
```

### Scenario 4: Grocery Cold Chain
**Use Case**: Frozen food delivery with temperature requirements

```bash
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "GROCERY_FROZEN_001",
    "latitude": 41.8781,
    "longitude": -87.6298,
    "orderItems": [
      {
        "sku": "FROZEN_PIZZA_001",
        "quantity": 5,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "FROZEN_FOOD_RULE",
        "scoringConfigurationId": "COLD_CHAIN_SCORING",
        ,
        "productCategory": "GROCERY_FROZEN",
        "specialHandling": "COLD_CHAIN",
        "temperatureRange": "FROZEN",
        "requireFullQuantity": true
      },
      {
        "sku": "ICE_CREAM_001",
        "quantity": 3,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "FROZEN_FOOD_RULE",
        "scoringConfigurationId": "COLD_CHAIN_SCORING",
        ,
        "productCategory": "GROCERY_FROZEN",
        "specialHandling": "COLD_CHAIN",
        "temperatureRange": "FROZEN"
      }
    ],
    "customerId": "GROCERY_CUST_001",
    "customerTier": "STANDARD",
    "orderType": "WEB",
    "requestedDeliveryDate": "2024-01-15T18:00:00"
  }'
```

### Scenario 5: Hazmat Shipping
**Use Case**: Hazardous materials with compliance requirements

```bash
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "HAZMAT_001",
    "latitude": 34.0522,
    "longitude": -118.2437,
    "orderItems": [
      {
        "sku": "BATTERY_LITHIUM_001",
        "quantity": 10,
        "deliveryType": "STANDARD",
        "locationFilterId": "HAZMAT_FILTER_RULE",
        "scoringConfigurationId": "HAZMAT_SCORING",
        ,
        "productCategory": "ELECTRONICS_BATTERY",
        "specialHandling": "HAZMAT",
        "hazmatClass": "UN3480",
        "requiresSignature": true,
        "requireFullQuantity": true
      }
    ],
    "customerId": "INDUSTRIAL_001",
    "customerTier": "BUSINESS",
    "orderType": "B2B",
    "salesChannel": "INDUSTRIAL_PORTAL"
  }'
```

### Scenario 6: Peak Season Rush
**Use Case**: High-demand period with capacity constraints

```bash
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "PEAK_SEASON_001",
    "latitude": 47.6062,
    "longitude": -122.3321,
    "orderItems": [
      {
        "sku": "GIFT_CARD_001",
        "quantity": 1,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "PEAK_SEASON_RULE",
        "scoringConfigurationId": "PEAK_SEASON_2024",
        ,
        "productCategory": "GIFT_CARDS",
        "isExpressPriority": true
      },
      {
        "sku": "PHONE123",
        "quantity": 2,
        "deliveryType": "STANDARD",
        "locationFilterId": "PEAK_SEASON_RULE",
        "scoringConfigurationId": "PEAK_SEASON_2024",
        "productCategory": "ELECTRONICS_MOBILE",
        "allowPartialFulfillment": true
      }
    ],
    "customerId": "HOLIDAY_CUST_001",
    "customerTier": "PREMIUM",
    "isPeakSeason": true,
    "allowPartialShipments": true,
    "orderType": "WEB",
    "campaignCode": "BLACK_FRIDAY_2024"
  }'
```

## 🔍 Advanced Scenarios

### Scenario 7: Multi-Location Split Fulfillment
**Use Case**: Large order requiring multiple locations

```bash
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "MULTI_LOCATION_001",
    "latitude": 42.3601,
    "longitude": -71.0589,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 75,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "productCategory": "ELECTRONICS_MOBILE",
        "allowPartialFulfillment": true,
        "preferSingleLocation": false
      }
    ],
    "customerId": "DISTRIBUTOR_001",
    "customerTier": "ENTERPRISE",
    "allowPartialShipments": true,
    "preferSingleLocation": false,
    "orderType": "B2B"
  }'
```

**Expected Multi-Location Response**:
```json
{
  "orderId": "MULTI_LOCATION_001",
  "processingTimeMs": 125,
  "fulfillmentPlans": [
    {
      "sku": "PHONE123",
      "requestedQuantity": 75,
      "totalFulfilled": 70,
      "isPartialFulfillment": true,
      "overallScore": 72.8,
      "locationAllocations": [
        {
          "locationId": 1,
          "locationName": "Downtown Warehouse",
          "allocatedQuantity": 50,
          "locationScore": 85.5,
          "deliveryTiming": {
            "estimatedShipDate": "2024-01-15T09:00:00",
            "estimatedDeliveryDate": "2024-01-18T17:00:00",
            "transitTimeDays": 3,
            "processingTimeHours": 24
          }
        },
        {
          "locationId": 2,
          "locationName": "Suburb Store",
          "allocatedQuantity": 20,
          "locationScore": 72.3,
          "deliveryTiming": {
            "estimatedShipDate": "2024-01-15T10:00:00",
            "estimatedDeliveryDate": "2024-01-19T17:00:00",
            "transitTimeDays": 4,
            "processingTimeHours": 48
          }
        }
      ]
    }
  ]
}
```

### Scenario 8: Performance Testing
**Use Case**: High-volume order for performance validation

```bash
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "PERFORMANCE_TEST_001",
    "latitude": 33.4484,
    "longitude": -112.0740,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 5,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
              },
      {
        "sku": "LAPTOP456",
        "quantity": 3,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
              },
      {
        "sku": "TABLET789",
        "quantity": 8,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
              },
      {
        "sku": "HEADPHONES101",
        "quantity": 12,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
              },
      {
        "sku": "PHONE123",
        "quantity": 7,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "SDD_FILTER_RULE",
              }
    ],
    "customerId": "PERF_TEST_USER",
    "customerTier": "STANDARD",
    "orderType": "WEB"
  }'
```

## 🛠️ Configuration Testing

### Get Available Scoring Configurations
```bash
curl -X GET http://localhost:8081/api/sourcing/scoring-configurations \
  -H "Accept: application/json"
```

### Get Specific Configuration Details
```bash
curl -X GET http://localhost:8081/api/sourcing/scoring-configurations/ELECTRONICS_PREMIUM_SCORING \
  -H "Accept: application/json"
```

### Validate Order Structure
```bash
curl -X POST http://localhost:8081/api/sourcing/source-realtime/validate \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "VALIDATION_TEST",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "TEST_SKU",
        "quantity": 1,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE"
      }
    ]
  }'
```

### Health Check
```bash
curl -X GET http://localhost:8081/api/sourcing/source-realtime/health \
  -H "Accept: application/json"
```

## 📊 Performance Expectations

| Scenario | Items | Expected Response Time | Strategy |
|----------|-------|----------------------|----------|
| PDP Single Item | 1 | 15-40ms | Sequential |
| Mixed Cart | 2-5 | 40-80ms | Batch |
| B2B Bulk | 5-10 | 80-150ms | Batch |
| Performance Test | 10+ | 150-300ms | Batch |

## 🔍 Error Handling Examples

### Invalid Location Filter
```bash
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "ERROR_TEST_001",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "STANDARD",
        "locationFilterId": "INVALID_FILTER_ID",
              }
    ]
  }'
```

**Error Response**:
```json
{
  "orderId": "ERROR_TEST_001",
  "processingTimeMs": 0,
  "fulfillmentPlans": []
}
```

### Missing Required Fields
```bash
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "ERROR_TEST_002",
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "STANDARD"
      }
    ]
  }'
```

## 🧪 A/B Testing Examples

### Test Different Scoring Configurations
```bash
# Configuration A - Default Scoring
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "AB_TEST_A_001",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "scoringConfigurationId": "DEFAULT_SCORING",
              }
    ]
  }'

# Configuration B - Custom Scoring
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "AB_TEST_B_001",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "scoringConfigurationId": "CUSTOM_TEST_SCORING",
              }
    ]
  }'
```

## 🚀 Integration Best Practices

### 1. Order ID Management
- Use unique `tempOrderId` for each request
- Include timestamp or session ID for traceability
- Format: `{SOURCE}_{TYPE}_{TIMESTAMP}_{SEQUENCE}`

### 2. Error Handling
- Always check `processingTimeMs` and `fulfillmentPlans` length
- Implement retry logic for timeouts
- Log scoring information for analysis

### 3. Performance Optimization
- Use simplified endpoint for UI applications
- Implement client-side caching for configuration data
- Monitor response times and adjust expectations

### 4. Testing Strategy
- Test with realistic data volumes
- Validate all delivery type combinations
- Test edge cases (no inventory, invalid locations)

This comprehensive guide provides all the API examples needed to integrate with the Order Sourcing Engine effectively across various business scenarios.