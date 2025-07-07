# Sample API Calls for Order Sourcing Engine

## Overview
This document provides comprehensive examples of API calls for the Order Sourcing Engine, including the new **Configurable Scoring System** that allows dynamic adjustment of scoring weights for different business scenarios.

## New Features: Configurable Scoring System

### Get All Scoring Configurations
```bash
curl -X GET http://localhost:8080/api/sourcing/scoring-configurations \
  -H "Accept: application/json"
```

### Get Specific Scoring Configuration
```bash
curl -X GET http://localhost:8080/api/sourcing/scoring-configurations/DEFAULT_SCORING \
  -H "Accept: application/json"
```

### Get Electronics Premium Scoring Configuration
```bash
curl -X GET http://localhost:8080/api/sourcing/scoring-configurations/ELECTRONICS_PREMIUM_SCORING \
  -H "Accept: application/json"
```

## Direct Order Sourcing with Configurable Scoring

### 1. Standard Order with Default Scoring

```bash
curl -X POST http://localhost:8080/api/sourcing/source-direct \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "STANDARD_ORDER_001",
    "customerId": "CUST001",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 2,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "scoringConfigurationId": "DEFAULT_SCORING",
        "unitPrice": 599.99,
        "isExpressPriority": false
      }
    ],
    "allowPartialShipments": true,
    "isExpressPriority": false
  }'
```

### 2. Electronics Order with Premium Scoring

```bash
curl -X POST http://localhost:8080/api/sourcing/source-direct \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "ELECTRONICS_ORDER_001",
    "customerId": "CUST002",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 2,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "scoringConfigurationId": "ELECTRONICS_PREMIUM_SCORING",
        "unitPrice": 1299.99,
        "productCategory": "ELECTRONICS",
        "isExpressPriority": false
      }
    ],
    "allowPartialShipments": true,
    "isExpressPriority": false
  }'
```

### 3. Express Delivery with Express Scoring

```bash
curl -X POST http://localhost:8080/api/sourcing/source-direct \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "EXPRESS_ORDER_001",
    "customerId": "CUST003",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "scoringConfigurationId": "EXPRESS_DELIVERY_SCORING",
        "unitPrice": 899.99,
        "isExpressPriority": true
      }
    ],
    "allowPartialShipments": false,
    "isExpressPriority": true
  }'
```

### 4. Hazmat Order with Specialized Scoring

```bash
curl -X POST http://localhost:8080/api/sourcing/source-direct \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "HAZMAT_ORDER_001",
    "customerId": "CUST004",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "scoringConfigurationId": "HAZMAT_SCORING",
        "unitPrice": 299.99,
        "isHazmat": true,
        "isExpressPriority": false
      }
    ],
    "allowPartialShipments": true,
    "isExpressPriority": false
  }'
```

## Available Scoring Configurations

The system includes 4 pre-configured scoring strategies:

### 1. DEFAULT_SCORING
- **Use Case**: General purpose orders
- **Transit Time Weight**: -10.0 (moderate penalty)
- **Inventory Weight**: 50.0 (standard preference)
- **Split Penalty**: 15.0 base (moderate multi-location penalty)

### 2. ELECTRONICS_PREMIUM_SCORING  
- **Use Case**: High-value electronics with security requirements
- **Transit Time Weight**: -15.0 (higher penalty for distance)
- **Inventory Weight**: 60.0 (prefers high-inventory locations)
- **Split Penalty**: 20.0 base (discourages splitting)
- **High Value Threshold**: $1000 (vs $500 default)

### 3. EXPRESS_DELIVERY_SCORING
- **Use Case**: Same-day and next-day delivery optimization
- **Transit Time Weight**: -20.0 (heavily penalizes distance)
- **Express Weight**: 50.0 (big bonus for fast locations)
- **Split Penalty**: 30.0 base (strongly discourages splitting)
- **Distance Threshold**: 50km (vs 100km default)

### 4. HAZMAT_SCORING
- **Use Case**: Hazardous materials with compliance requirements
- **Custom Script**: Includes conditional logic for hazmat items
- **Confidence Adjustments**: Lower base confidence (70% vs 80%)
- **Same Day Penalty**: 50.0 (highest urgency penalty)
- **Script**: `base = location.transitTime * scoring.transitTimeWeight + inventory.ratio * scoring.inventoryWeight; return order.isHazmat ? base * 0.8 : base`

## Scoring Comparison Example

When ordering the same item with different scoring configurations, you'll see different location preferences:

| Location | DEFAULT | ELECTRONICS | EXPRESS | HAZMAT |
|----------|---------|-------------|---------|--------|
| Downtown (1 day) | 35.0 | 37.0 | 15.0 | 33.6 |
| Suburb (2 days) | 20.0 | 14.0 | -10.0 | 16.0 |
| Regional (3 days) | 10.0 | -1.0 | -25.0 | 8.0 |

This demonstrates how different configurations optimize for different business priorities.

## Legacy API Calls (For Backward Compatibility)

### 5. Single Item PDP Page Request (Same Day Delivery)

## 2. Multi-Item Cart/Checkout Request (Mixed Delivery Types)

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "CART_SESSION_67890",
    "latitude": 40.7489,
    "longitude": -73.9857,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 2,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "SDD_FILTER_RULE",
        "unitPrice": 299.99,
        "productCategory": "ELECTRONICS_MOBILE"
      },
      {
        "sku": "LAPTOP456",
        "quantity": 1,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "unitPrice": 1299.99,
        "productCategory": "ELECTRONICS_COMPUTER",
        "specialHandling": "HIGH_VALUE"
      },
      {
        "sku": "HEADPHONES101",
        "quantity": 3,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "unitPrice": 99.99,
        "productCategory": "ELECTRONICS_AUDIO"
      }
    ],
    "customerId": "CUST_002",
    "customerTier": "STANDARD",
    "isPeakSeason": true,
    "allowPartialShipments": true,
    "preferSingleLocation": false,
    "orderType": "WEB"
  }'
```

## 3. High-Value Electronics Order (Security Requirements)

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "HIGH_VALUE_001",
    "latitude": 41.8781,
    "longitude": -87.6298,
    "orderItems": [
      {
        "sku": "LAPTOP456",
        "quantity": 1,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "unitPrice": 2499.99,
        "productCategory": "ELECTRONICS_COMPUTER",
        "specialHandling": "SIGNATURE_REQUIRED"
      }
    ],
    "customerId": "CUST_003",
    "customerTier": "PREMIUM",
    "orderPriority": 1,
    "allowBackorders": false,
    "orderType": "B2B"
  }'
```

## 4. Frozen Food Order (Temperature Controlled)

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "FROZEN_ORDER_001",
    "latitude": 34.0522,
    "longitude": -118.2437,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 5,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "FROZEN_FOOD_RULE",
        "unitPrice": 12.99,
        "productCategory": "FOOD_FROZEN",
        "requiresColdStorage": true,
        "specialHandling": "TEMPERATURE_CONTROLLED"
      },
      {
        "sku": "HEADPHONES101",
        "quantity": 2,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "FROZEN_FOOD_RULE",
        "unitPrice": 8.99,
        "productCategory": "FOOD_FROZEN",
        "requiresColdStorage": true
      }
    ],
    "customerId": "CUST_004",
    "customerTier": "STANDARD",
    "requestedDeliveryDate": "2025-07-03T18:00:00",
    "timeZone": "America/Los_Angeles"
  }'
```

## 5. Large B2B Order (Peak Season)

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "B2B_BULK_001",
    "latitude": 39.9526,
    "longitude": -75.1652,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 25,
        "deliveryType": "STANDARD",
        "locationFilterId": "PEAK_SEASON_RULE",
        "unitPrice": 299.99,
        "productCategory": "ELECTRONICS_MOBILE"
      },
      {
        "sku": "LAPTOP456",
        "quantity": 10,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "unitPrice": 1299.99,
        "productCategory": "ELECTRONICS_COMPUTER"
      },
      {
        "sku": "TABLET789",
        "quantity": 15,
        "deliveryType": "STANDARD",
        "locationFilterId": "PEAK_SEASON_RULE",
        "unitPrice": 599.99,
        "productCategory": "ELECTRONICS_TABLET"
      }
    ],
    "customerId": "B2B_CORP_001",
    "customerTier": "ENTERPRISE",
    "isPeakSeason": true,
    "allowPartialShipments": false,
    "preferSingleLocation": true,
    "orderType": "B2B",
    "orderPriority": 2,
    "salesChannel": "B2B"
  }'
```

## 11. Order Validation Request

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime/validate \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "VALIDATION_TEST_001",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "SDD_FILTER_RULE",
        "unitPrice": 299.99
      }
    ]
  }'
```

## 12. Health Check Request

```bash
curl -X GET http://localhost:8080/api/sourcing/source-realtime/health \
  -H "Accept: application/json"
```

## 13. Hazmat Item Request (Special Handling)

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "HAZMAT_ORDER_001",
    "latitude": 29.7604,
    "longitude": -95.3698,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "unitPrice": 49.99,
        "productCategory": "CHEMICAL_BATTERY",
        "isHazmat": true,
        "specialHandling": "HAZMAT_CERTIFIED"
      }
    ],
    "customerId": "CUST_005",
    "customerTier": "STANDARD",
    "orderType": "WEB"
  }'
```

## 9. Partial Allocation Test (Insufficient Inventory)

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "PARTIAL_ALLOCATION_TEST_001",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 200,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "unitPrice": 299.99,
        "productCategory": "ELECTRONICS_MOBILE",
        "allowPartialFulfillment": true,
        "preferSingleLocation": false
      },
      {
        "sku": "LAPTOP456",
        "quantity": 100,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "unitPrice": 1299.99,
        "productCategory": "ELECTRONICS_COMPUTER",
        "allowPartialFulfillment": false,
        "requireFullQuantity": true,
        "preferSingleLocation": true
      },
      {
        "sku": "TABLET789",
        "quantity": 150,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "SDD_FILTER_RULE",
        "unitPrice": 599.99,
        "productCategory": "ELECTRONICS_TABLET",
        "allowPartialFulfillment": true,
        "allowBackorder": true,
        "preferSingleLocation": false
      }
    ],
    "customerId": "PARTIAL_TEST_USER",
    "customerTier": "PREMIUM",
    "orderType": "WEB"
  }'
```

## 10. Item-Level Fulfillment Controls Test

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "ITEM_CONTROLS_TEST_001",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 30,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "unitPrice": 299.99,
        "allowPartialFulfillment": false,
        "requireFullQuantity": true
      },
      {
        "sku": "LAPTOP456",
        "quantity": 5,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "unitPrice": 1299.99,
        "allowPartialFulfillment": true,
        "preferSingleLocation": true
      },
      {
        "sku": "HEADPHONES101",
        "quantity": 200,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "SDD_FILTER_RULE",
        "unitPrice": 99.99,
        "allowPartialFulfillment": true,
        "allowBackorder": true,
        "preferSingleLocation": false
      }
    ],
    "customerId": "CONTROLS_TEST_USER",
    "customerTier": "STANDARD",
    "orderType": "WEB"
  }'
```

## 11. Performance Test Request (20 Items)

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "PERFORMANCE_TEST_001",
    "latitude": 47.6062,
    "longitude": -122.3321,
    "orderItems": [
      {"sku": "PHONE123", "quantity": 1, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 299.99},
      {"sku": "LAPTOP456", "quantity": 1, "deliveryType": "SAME_DAY", "locationFilterId": "SDD_FILTER_RULE", "unitPrice": 1299.99},
      {"sku": "TABLET789", "quantity": 1, "deliveryType": "NEXT_DAY", "locationFilterId": "ELECTRONICS_SECURE_RULE", "unitPrice": 599.99},
      {"sku": "HEADPHONES101", "quantity": 2, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 99.99},
      {"sku": "PHONE123", "quantity": 1, "deliveryType": "SAME_DAY", "locationFilterId": "SDD_FILTER_RULE", "unitPrice": 299.99},
      {"sku": "LAPTOP456", "quantity": 1, "deliveryType": "STANDARD", "locationFilterId": "PEAK_SEASON_RULE", "unitPrice": 1299.99},
      {"sku": "TABLET789", "quantity": 1, "deliveryType": "NEXT_DAY", "locationFilterId": "ELECTRONICS_SECURE_RULE", "unitPrice": 599.99},
      {"sku": "HEADPHONES101", "quantity": 3, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 99.99},
      {"sku": "PHONE123", "quantity": 2, "deliveryType": "SAME_DAY", "locationFilterId": "SDD_FILTER_RULE", "unitPrice": 299.99},
      {"sku": "LAPTOP456", "quantity": 1, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 1299.99},
      {"sku": "TABLET789", "quantity": 2, "deliveryType": "NEXT_DAY", "locationFilterId": "ELECTRONICS_SECURE_RULE", "unitPrice": 599.99},
      {"sku": "HEADPHONES101", "quantity": 2, "deliveryType": "STANDARD", "locationFilterId": "PEAK_SEASON_RULE", "unitPrice": 99.99},
      {"sku": "PHONE123", "quantity": 1, "deliveryType": "SAME_DAY", "locationFilterId": "SDD_FILTER_RULE", "unitPrice": 299.99},
      {"sku": "LAPTOP456", "quantity": 1, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 1299.99},
      {"sku": "TABLET789", "quantity": 1, "deliveryType": "NEXT_DAY", "locationFilterId": "ELECTRONICS_SECURE_RULE", "unitPrice": 599.99},
      {"sku": "HEADPHONES101", "quantity": 4, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 99.99},
      {"sku": "PHONE123", "quantity": 1, "deliveryType": "SAME_DAY", "locationFilterId": "SDD_FILTER_RULE", "unitPrice": 299.99},
      {"sku": "LAPTOP456", "quantity": 1, "deliveryType": "STANDARD", "locationFilterId": "PEAK_SEASON_RULE", "unitPrice": 1299.99},
      {"sku": "TABLET789", "quantity": 1, "deliveryType": "NEXT_DAY", "locationFilterId": "ELECTRONICS_SECURE_RULE", "unitPrice": 599.99},
      {"sku": "HEADPHONES101", "quantity": 2, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 99.99}
    ],
    "customerId": "PERF_TEST_USER",
    "customerTier": "STANDARD",
    "orderType": "WEB",
    "isPeakSeason": false
  }'
```

## Expected Response Times

- **Single Item (PDP)**: ~50-100ms
- **Multi-Item (3-5 items)**: ~100-200ms  
- **Large Orders (10+ items)**: ~200-500ms
- **Performance Test (20 items)**: ~300-600ms

## Response Structure

All requests return a `SourcingResponse` with:

```json
{
  "tempOrderId": "ORDER_ID",
  "fulfillmentPlans": [
    {
      "sku": "ITEM_SKU",
      "quantity": 1,
      "location": {
        "id": 1,
        "name": "Location Name",
        "distanceFromCustomer": 25.5
      },
      "promiseDates": {
        "promiseDate": "2025-06-29T17:00:00",
        "systemProcessingHours": 1,
        "locationProcessingHours": 2,
        "carrierTransitHours": 8,
        "confidenceScore": 0.95,
        "carrierCode": "UPS"
      },
      "quantityPromises": [
        {
          "quantity": 1,
          "promiseDate": "2025-06-29T17:00:00",
          "estimatedShipDate": "2025-06-29T09:00:00",
          "estimatedDeliveryDate": "2025-06-29T15:00:00"
        }
      ]
    }
  ],
  "metadata": {
    "totalProcessingTimeMs": 125,
    "processingStrategy": "BATCH",
    "filtersExecuted": 3,
    "totalLocationsEvaluated": 45
  }
}
```