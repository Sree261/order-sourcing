# Sample API Requests for Order Sourcing Engine

This document contains optimized sample API requests demonstrating different order sourcing scenarios with item-level fulfillment controls.

## 1. PDP Single Item Request (Same Day Delivery)

**Use Case**: Product Detail Page - single item availability check for same-day delivery

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "PDP_SESSION_12345",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "SDD_FILTER_RULE",
        "unitPrice": 299.99,
        "productCategory": "ELECTRONICS_MOBILE",
        "isExpressPriority": true,
        "allowPartialFulfillment": false,
        "preferSingleLocation": true
      }
    ],
    "customerId": "CUST_001",
    "customerTier": "PREMIUM",
    "orderType": "WEB"
  }'
```

**Expected Response Time**: ~30-80ms  
**Strategy**: Sequential processing, single location fulfillment

---

## 2. Mixed Basket with Multiple Delivery Types

**Use Case**: Shopping cart with items requiring different delivery speeds and fulfillment policies

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "MIXED_CART_67890",
    "latitude": 40.7489,
    "longitude": -73.9857,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 2,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "SDD_FILTER_RULE",
        "unitPrice": 299.99,
        "productCategory": "ELECTRONICS_MOBILE",
        "allowPartialFulfillment": true,
        "preferSingleLocation": false
      },
      {
        "sku": "LAPTOP456",
        "quantity": 1,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "unitPrice": 1299.99,
        "productCategory": "ELECTRONICS_COMPUTER",
        "specialHandling": "HIGH_VALUE",
        "allowPartialFulfillment": false,
        "requireFullQuantity": true,
        "preferSingleLocation": true
      },
      {
        "sku": "HEADPHONES101",
        "quantity": 3,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "unitPrice": 99.99,
        "productCategory": "ELECTRONICS_AUDIO",
        "allowPartialFulfillment": true,
        "allowBackorder": true,
        "preferSingleLocation": false
      }
    ],
    "customerId": "CUST_002",
    "customerTier": "STANDARD",
    "allowPartialShipments": true,
    "preferSingleLocation": false,
    "orderType": "WEB"
  }'
```

**Expected Response Time**: ~80-150ms  
**Strategy**: Batch processing, mixed fulfillment strategies per item

---

## 3. Large B2B Order (Bulk Purchase)

**Use Case**: Enterprise bulk order with high quantities and strict fulfillment requirements

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
        "quantity": 50,
        "deliveryType": "STANDARD",
        "locationFilterId": "PEAK_SEASON_RULE",
        "unitPrice": 299.99,
        "productCategory": "ELECTRONICS_MOBILE",
        "allowPartialFulfillment": true,
        "preferSingleLocation": true
      },
      {
        "sku": "LAPTOP456",
        "quantity": 25,
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
        "quantity": 35,
        "deliveryType": "STANDARD",
        "locationFilterId": "PEAK_SEASON_RULE",
        "unitPrice": 599.99,
        "productCategory": "ELECTRONICS_TABLET",
        "allowPartialFulfillment": true,
        "allowBackorder": true,
        "preferSingleLocation": false
      }
    ],
    "customerId": "B2B_CORP_001",
    "customerTier": "ENTERPRISE",
    "isPeakSeason": true,
    "allowPartialShipments": false,
    "preferSingleLocation": true,
    "orderType": "B2B",
    "orderPriority": 1,
    "salesChannel": "B2B"
  }'
```

**Expected Response Time**: ~150-400ms  
**Strategy**: Batch processing, multi-location fulfillment with split penalties

---

## 4. Performance Test Request (High Load Simulation)

**Use Case**: Stress testing with multiple items to evaluate system performance under load

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "PERFORMANCE_TEST_001",
    "latitude": 47.6062,
    "longitude": -122.3321,
    "orderItems": [
      {"sku": "PHONE123", "quantity": 5, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 299.99, "allowPartialFulfillment": true},
      {"sku": "LAPTOP456", "quantity": 3, "deliveryType": "SAME_DAY", "locationFilterId": "SDD_FILTER_RULE", "unitPrice": 1299.99, "preferSingleLocation": true},
      {"sku": "TABLET789", "quantity": 8, "deliveryType": "NEXT_DAY", "locationFilterId": "ELECTRONICS_SECURE_RULE", "unitPrice": 599.99, "allowPartialFulfillment": true},
      {"sku": "HEADPHONES101", "quantity": 12, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 99.99, "allowPartialFulfillment": true},
      {"sku": "PHONE123", "quantity": 7, "deliveryType": "SAME_DAY", "locationFilterId": "SDD_FILTER_RULE", "unitPrice": 299.99, "preferSingleLocation": true},
      {"sku": "LAPTOP456", "quantity": 4, "deliveryType": "STANDARD", "locationFilterId": "PEAK_SEASON_RULE", "unitPrice": 1299.99, "requireFullQuantity": true},
      {"sku": "TABLET789", "quantity": 6, "deliveryType": "NEXT_DAY", "locationFilterId": "ELECTRONICS_SECURE_RULE", "unitPrice": 599.99, "allowPartialFulfillment": true},
      {"sku": "HEADPHONES101", "quantity": 15, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 99.99, "allowPartialFulfillment": true},
      {"sku": "PHONE123", "quantity": 3, "deliveryType": "SAME_DAY", "locationFilterId": "SDD_FILTER_RULE", "unitPrice": 299.99, "allowPartialFulfillment": false},
      {"sku": "LAPTOP456", "quantity": 2, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 1299.99, "preferSingleLocation": true},
      {"sku": "TABLET789", "quantity": 10, "deliveryType": "NEXT_DAY", "locationFilterId": "ELECTRONICS_SECURE_RULE", "unitPrice": 599.99, "allowPartialFulfillment": true},
      {"sku": "HEADPHONES101", "quantity": 20, "deliveryType": "STANDARD", "locationFilterId": "PEAK_SEASON_RULE", "unitPrice": 99.99, "allowBackorder": true},
      {"sku": "PHONE123", "quantity": 4, "deliveryType": "SAME_DAY", "locationFilterId": "SDD_FILTER_RULE", "unitPrice": 299.99, "requireFullQuantity": true},
      {"sku": "LAPTOP456", "quantity": 1, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 1299.99, "allowPartialFulfillment": true},
      {"sku": "TABLET789", "quantity": 9, "deliveryType": "NEXT_DAY", "locationFilterId": "ELECTRONICS_SECURE_RULE", "unitPrice": 599.99, "preferSingleLocation": false},
      {"sku": "HEADPHONES101", "quantity": 25, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 99.99, "allowPartialFulfillment": true},
      {"sku": "PHONE123", "quantity": 6, "deliveryType": "SAME_DAY", "locationFilterId": "SDD_FILTER_RULE", "unitPrice": 299.99, "allowPartialFulfillment": true},
      {"sku": "LAPTOP456", "quantity": 5, "deliveryType": "STANDARD", "locationFilterId": "PEAK_SEASON_RULE", "unitPrice": 1299.99, "preferSingleLocation": true},
      {"sku": "TABLET789", "quantity": 7, "deliveryType": "NEXT_DAY", "locationFilterId": "ELECTRONICS_SECURE_RULE", "unitPrice": 599.99, "allowPartialFulfillment": false},
      {"sku": "HEADPHONES101", "quantity": 18, "deliveryType": "STANDARD", "locationFilterId": "STANDARD_DELIVERY_RULE", "unitPrice": 99.99, "allowBackorder": true}
    ],
    "customerId": "PERF_TEST_USER",
    "customerTier": "STANDARD",
    "orderType": "WEB",
    "isPeakSeason": false
  }'
```

**Expected Response Time**: ~200-500ms  
**Strategy**: Batch processing with maximum parallelization and caching utilization

---

## Expected Response Structure

All requests return a comprehensive `SourcingResponse` with item-centric fulfillment plans:

```json
{
  "tempOrderId": "ORDER_ID",
  "fulfillmentPlans": [
    {
      "sku": "PHONE123",
      "requestedQuantity": 5,
      "deliveryType": "STANDARD",
      "locationFilterId": "STANDARD_DELIVERY_RULE",
      "unitPrice": 299.99,
      "totalFulfilled": 5,
      "remainingQuantity": 0,
      "isPartialFulfillment": false,
      "isMultiLocationFulfillment": false,
      "recommendedStrategy": "SINGLE_LOCATION",
      "overallScore": 85.5,
      "splitPenalty": 0.0,
      "locationFulfillments": [
        {
          "location": {
            "id": 1,
            "name": "Downtown Warehouse",
            "type": "WAREHOUSE",
            "distanceFromCustomer": 8.7
          },
          "availableInventory": 50,
          "allocatedQuantity": 5,
          "canFulfillCompletely": true,
          "locationScore": 85.5,
          "allocationPriority": 1,
          "isPrimaryLocation": true,
          "isAllocatedInOptimalPlan": true,
          "fulfillmentStatus": "FULL"
        }
      ]
    }
  ],
  "metadata": {
    "totalProcessingTimeMs": 125,
    "processingStrategy": "BATCH",
    "filtersExecuted": 3,
    "totalLocationsEvaluated": 15
  }
}
```

## Performance Benchmarks

- **PDP Single Item**: ~30-80ms (Cache hit: ~15-30ms)
- **Mixed Basket (3-5 items)**: ~80-150ms (Cache hit: ~40-80ms)
- **Large B2B Order**: ~150-400ms (Cache hit: ~80-200ms)
- **Performance Test (20 items)**: ~200-500ms (Cache hit: ~100-250ms)

## Health Check

```bash
curl -X GET http://localhost:8080/api/sourcing/source-realtime/health \
  -H "Accept: application/json"
```

## Validation Endpoint

```bash
curl -X POST http://localhost:8080/api/sourcing/source-realtime/validate \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "VALIDATION_TEST",
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