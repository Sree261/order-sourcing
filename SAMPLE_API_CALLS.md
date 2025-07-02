# Sample API Calls for Real-Time Order Sourcing

## 1. Single Item PDP Page Request (Same Day Delivery)

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
        "isExpressPriority": true
      }
    ],
    "customerId": "CUST_001",
    "customerTier": "PREMIUM",
    "orderType": "WEB"
  }'
```

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