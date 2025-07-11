# Order Sourcing Engine - API Examples

## API Endpoint

**Base URL:** `http://localhost:8081/api/sourcing`  
**Endpoint:** `POST /source`  
**Content-Type:** `application/json`

## Request Structure

```json
{
  "tempOrderId": "string",           // Unique order identifier
  "latitude": 40.7128,              // Customer latitude
  "longitude": -74.0060,            // Customer longitude  
  "orderItems": [                   // Array of items to source
    {
      "sku": "string",              // Product SKU
      "quantity": 1,                // Requested quantity
      "deliveryType": "STANDARD",   // STANDARD, NEXT_DAY, SAME_DAY
      "locationFilterId": "string", // Location filter rule ID
      "scoringConfigurationId": "string", // Optional scoring config
      "productCategory": "string",   // Product category
      "allowPartialFulfillment": true,
      "preferSingleLocation": false,
      "requiresSignature": false,
      "specialHandling": "string"
    }
  ],
  "customerId": "string",           // Customer identifier
  "customerTier": "STANDARD",       // STANDARD, PREMIUM, ENTERPRISE
  "allowPartialShipments": true,
  "preferSingleLocation": false,
  "orderType": "WEB",              // WEB, B2B, MOBILE
  "requestTimestamp": "2024-01-15T10:00:00"
}
```

## Response Structure

```json
{
  "orderId": "string",
  "processingTimeMs": 45,
  "fulfillmentPlans": [
    {
      "sku": "string",
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

## Scenario Examples

### 1. Product Detail Page (PDP) Request

**Use Case:** Customer viewing a single product, checking availability

```bash
curl -X POST http://localhost:8081/api/sourcing/source \
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

**Expected Response Time:** 15-40ms (Sequential Processing)

### 2. Mixed Shopping Cart

**Use Case:** Customer with multiple items, different delivery requirements

```bash
curl -X POST http://localhost:8081/api/sourcing/source \
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
        "requiresSignature": false
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

**Expected Response Time:** 40-80ms (Batch Processing)

### 3. B2B Bulk Order

**Use Case:** Enterprise customer placing large quantity order

```bash
curl -X POST http://localhost:8081/api/sourcing/source \
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
        "locationFilterId": "B2B_DELIVERY_RULE",
        "scoringConfigurationId": "B2B_BULK_SCORING",
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
        "productCategory": "ELECTRONICS_COMPUTER",
        "requiresSignature": true,
        "specialHandling": "CORPORATE_DELIVERY"
      }
    ],
    "customerId": "CORP_001",
    "customerTier": "ENTERPRISE",
    "allowPartialShipments": true,
    "preferSingleLocation": false,
    "orderType": "B2B"
  }'
```

**Expected Response Time:** 80-150ms (Batch Processing)

### 4. Same-Day Delivery Rush

**Use Case:** Customer needs urgent same-day delivery

```bash
curl -X POST http://localhost:8081/api/sourcing/source \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "SAME_DAY_RUSH_001",
    "latitude": 34.0522,
    "longitude": -118.2437,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "SDD_FILTER_RULE",
        "scoringConfigurationId": "EXPRESS_DELIVERY_SCORING",
        "productCategory": "ELECTRONICS_MOBILE",
        "requiresSignature": false
      }
    ],
    "customerId": "URGENT_CUST_001",
    "customerTier": "PREMIUM",
    "orderType": "WEB",
    "requestTimestamp": "2024-01-15T10:00:00"
  }'
```

**Expected Response Time:** 15-30ms (Sequential Processing)

### 5. Electronics High-Value Order

**Use Case:** Premium electronics requiring special handling

```bash
curl -X POST http://localhost:8081/api/sourcing/source \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "ELECTRONICS_PREMIUM_001",
    "latitude": 37.7749,
    "longitude": -122.4194,
    "orderItems": [
      {
        "sku": "LAPTOP_PRO_001",
        "quantity": 1,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "scoringConfigurationId": "ELECTRONICS_PREMIUM_SCORING",
        "productCategory": "ELECTRONICS_COMPUTER",
        "specialHandling": "HIGH_VALUE",
        "requiresSignature": true
      },
      {
        "sku": "MONITOR_4K_001",
        "quantity": 2,
        "deliveryType": "STANDARD",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "scoringConfigurationId": "ELECTRONICS_PREMIUM_SCORING", 
        "productCategory": "ELECTRONICS_DISPLAY",
        "specialHandling": "FRAGILE"
      }
    ],
    "customerId": "PREMIUM_CUST_001",
    "customerTier": "PREMIUM",
    "allowPartialShipments": false,
    "preferSingleLocation": true,
    "orderType": "WEB"
  }'
```

**Expected Response Time:** 45-70ms (Batch Processing)

### 6. Peak Season Order

**Use Case:** Holiday season order with capacity constraints

```bash
curl -X POST http://localhost:8081/api/sourcing/source \
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
        "scoringConfigurationId": "PEAK_SEASON_SCORING",
        "productCategory": "GIFT_CARDS"
      },
      {
        "sku": "PHONE123",
        "quantity": 2,
        "deliveryType": "STANDARD", 
        "locationFilterId": "PEAK_SEASON_RULE",
        "scoringConfigurationId": "PEAK_SEASON_SCORING",
        "productCategory": "ELECTRONICS_MOBILE",
        "allowPartialFulfillment": true
      },
      {
        "sku": "TOY_001",
        "quantity": 3,
        "deliveryType": "STANDARD",
        "locationFilterId": "PEAK_SEASON_RULE",
        "scoringConfigurationId": "PEAK_SEASON_SCORING",
        "productCategory": "TOYS"
      }
    ],
    "customerId": "HOLIDAY_CUST_001",
    "customerTier": "STANDARD",
    "isPeakSeason": true,
    "allowPartialShipments": true,
    "orderType": "WEB"
  }'
```

**Expected Response Time:** 60-100ms (Batch Processing)

### 7. Large Performance Test Order

**Use Case:** Stress testing with many items

```bash
curl -X POST http://localhost:8081/api/sourcing/source \
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
        "productCategory": "ELECTRONICS_MOBILE"
      },
      {
        "sku": "LAPTOP456",
        "quantity": 3,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "productCategory": "ELECTRONICS_COMPUTER"
      },
      {
        "sku": "TABLET789",
        "quantity": 8,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "productCategory": "ELECTRONICS_TABLET"
      },
      {
        "sku": "HEADPHONES101",
        "quantity": 12,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "productCategory": "ELECTRONICS_AUDIO"
      },
      {
        "sku": "MONITOR_001",
        "quantity": 4,
        "deliveryType": "STANDARD",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "productCategory": "ELECTRONICS_DISPLAY"
      },
      {
        "sku": "KEYBOARD_001",
        "quantity": 10,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "productCategory": "ELECTRONICS_ACCESSORY"
      },
      {
        "sku": "MOUSE_001",
        "quantity": 15,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "productCategory": "ELECTRONICS_ACCESSORY"
      }
    ],
    "customerId": "PERF_TEST_USER",
    "customerTier": "STANDARD",
    "allowPartialShipments": true,
    "orderType": "WEB"
  }'
```

**Expected Response Time:** 150-300ms (Batch Processing)

### 8. Multi-Location Split Required

**Use Case:** Large quantity requiring multiple locations

```bash
curl -X POST http://localhost:8081/api/sourcing/source \
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

**Expected Response Time:** 80-120ms (Batch Processing)

## Example Response Scenarios

### Single Location Fulfillment

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

### Multi-Location Split Fulfillment

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
          "locationName": "Suburb Distribution Center",
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

### Partial Fulfillment (Insufficient Inventory)

```json
{
  "orderId": "PARTIAL_001",
  "processingTimeMs": 58,
  "fulfillmentPlans": [
    {
      "sku": "LIMITED_ITEM_001",
      "requestedQuantity": 10,
      "totalFulfilled": 7,
      "isPartialFulfillment": true,
      "overallScore": 78.2,
      "locationAllocations": [
        {
          "locationId": 3,
          "locationName": "Regional Hub",
          "allocatedQuantity": 7,
          "locationScore": 78.2,
          "deliveryTiming": {
            "estimatedShipDate": "2024-01-15T14:00:00",
            "estimatedDeliveryDate": "2024-01-17T17:00:00",
            "transitTimeDays": 2,
            "processingTimeHours": 6
          }
        }
      ]
    }
  ]
}
```

### Error Response (Invalid Location Filter)

```json
{
  "orderId": "ERROR_TEST_001",
  "processingTimeMs": 0,
  "fulfillmentPlans": []
}
```

## Performance Benchmarks

| Scenario | Items | Avg Response Time | Processing Strategy |
|----------|-------|-------------------|-------------------|
| PDP Request | 1 | 25ms | Sequential |
| Small Cart | 2-3 | 45ms | Sequential/Batch |
| Mixed Cart | 3-5 | 65ms | Batch |
| B2B Order | 5-10 | 120ms | Batch |
| Large Order | 10+ | 250ms | Batch |

## Testing Helper Scripts

### Quick Health Check
```bash
#!/bin/bash
echo "Testing Order Sourcing Engine..."
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health)
if [ $response -eq 200 ]; then
    echo "✅ Service is healthy"
else
    echo "❌ Service is down (HTTP $response)"
fi
```

### Performance Test Script
```bash
#!/bin/bash
echo "Running performance tests..."

# Test PDP performance
start=$(date +%s%N)
curl -s -X POST http://localhost:8081/api/sourcing/source \
  -H "Content-Type: application/json" \
  -d '{"tempOrderId":"PERF_PDP","latitude":40.7128,"longitude":-74.0060,"orderItems":[{"sku":"PHONE123","quantity":1,"deliveryType":"STANDARD","locationFilterId":"STANDARD_DELIVERY_RULE"}],"customerId":"PERF_TEST","orderType":"WEB"}' > /dev/null
end=$(date +%s%N)
pdp_time=$(( (end - start) / 1000000 ))

echo "PDP Response Time: ${pdp_time}ms"
```

## Error Scenarios

### Missing Required Fields
```bash
curl -X POST http://localhost:8081/api/sourcing/source \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "ERROR_TEST_002",
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "STANDARD"
        // Missing locationFilterId - will return 400 error
      }
    ]
  }'
```

### Invalid Coordinates
```bash
curl -X POST http://localhost:8081/api/sourcing/source \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "ERROR_TEST_003",
    "latitude": 200.0,  // Invalid latitude
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE"
      }
    ]
  }'
```

## Same-Day Delivery Configuration

### Database Configuration for Same-Day Delivery

Before using same-day delivery, add these configurations to your database:

#### Scoring Configuration - Prioritizes Nearest Location
```sql
-- Same-Day Delivery Scoring Configuration
INSERT INTO scoring_configuration (
    id, name, description, is_active,
    transit_time_weight, processing_time_weight, inventory_weight,
    express_weight, distance_weight, split_penalty_base,
    split_penalty_exponent, split_penalty_multiplier,
    high_value_threshold, high_value_penalty,
    same_day_penalty, next_day_penalty,
    distance_threshold, base_confidence,
    peak_season_adjustment, weather_adjustment, hazmat_adjustment
) VALUES (
    'SAME_DAY_DELIVERY_SCORING',
    'Same-Day Delivery Optimization',
    'Optimized for same-day delivery - prioritizes nearest locations',
    true,
    -20.0,  -- Strong preference for fast transit
    -15.0,  -- Strong preference for fast processing
    40.0,   -- Moderate inventory importance
    30.0,   -- High express delivery bonus
    -5.0,   -- VERY strong distance penalty (nearest location priority)
    50.0,   -- High split penalty (prefer single location)
    2.5,    -- Exponential split penalty
    25.0,   -- Split penalty multiplier
    1000.0, -- High value threshold
    30.0,   -- High value penalty
    0.0,    -- No same-day penalty (this IS same-day)
    20.0,   -- Next-day penalty
    25.0,   -- Distance threshold (km)
    0.9,    -- High base confidence
    -0.2,   -- Peak season adjustment
    -0.1,   -- Weather adjustment
    -0.3    -- Hazmat adjustment
);
```

#### Location Filter - Same-Day Delivery Rules
```sql
-- Same-Day Delivery Location Filter
INSERT INTO location_filter (
    id, name, description, filter_script, is_active, priority, cache_ttl_minutes
) VALUES (
    'SAME_DAY_DELIVERY_FILTER',
    'Same-Day Delivery Location Filter',
    'Filters locations for same-day delivery capability with distance and time constraints',
    'calculateDistance(location.latitude, location.longitude, order.latitude, order.longitude) <= 30 && location.transitTime <= 1',
    true,
    1,
    30  -- 30 minute cache
);
```

### Same-Day Delivery API Example

**Use Case:** Customer needs same-day delivery, system should prioritize the nearest available location

```bash
curl -X POST http://localhost:8081/api/sourcing/source \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "SAME_DAY_OPTIMIZED_001",
    "latitude": 40.7589,
    "longitude": -73.9851,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "SAME_DAY_DELIVERY_FILTER",
        "scoringConfigurationId": "SAME_DAY_DELIVERY_SCORING",
        "productCategory": "ELECTRONICS_MOBILE",
        "requiresSignature": false,
        "allowPartialFulfillment": false,
        "preferSingleLocation": true
      }
    ],
    "customerId": "URGENT_CUSTOMER_001",
    "customerTier": "PREMIUM",
    "allowPartialShipments": false,
    "preferSingleLocation": true,
    "orderType": "WEB",
    "requestTimestamp": "2024-01-15T11:30:00"
  }'
```

**Expected Response Time:** 15-30ms (Sequential Processing)
**Expected Behavior:** 
- Only considers locations within 30km radius
- Only locations with transit time ≤ 1 day
- Heavily penalizes distance (nearest location wins)
- Prefers single location fulfillment
- High confidence in delivery promises

### Multiple Items Same-Day Delivery

```bash
curl -X POST http://localhost:8081/api/sourcing/source \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "SAME_DAY_MULTI_001",
    "latitude": 34.0522,
    "longitude": -118.2437,
    "orderItems": [
      {
        "sku": "PHONE123",
        "quantity": 1,
        "deliveryType": "SAME_DAY",
        "locationFilterId": "SAME_DAY_DELIVERY_FILTER",
        "scoringConfigurationId": "SAME_DAY_DELIVERY_SCORING",
        "productCategory": "ELECTRONICS_MOBILE"
      },
      {
        "sku": "HEADPHONES101",
        "quantity": 1,
        "deliveryType": "SAME_DAY", 
        "locationFilterId": "SAME_DAY_DELIVERY_FILTER",
        "scoringConfigurationId": "SAME_DAY_DELIVERY_SCORING",
        "productCategory": "ELECTRONICS_AUDIO"
      }
    ],
    "customerId": "SAME_DAY_CUSTOMER_002",
    "customerTier": "STANDARD",
    "allowPartialShipments": false,
    "preferSingleLocation": true,
    "orderType": "WEB"
  }'
```

### Expected Response for Same-Day Delivery

```json
{
  "orderId": "SAME_DAY_OPTIMIZED_001",
  "processingTimeMs": 22,
  "fulfillmentPlans": [
    {
      "sku": "PHONE123",
      "requestedQuantity": 1,
      "totalFulfilled": 1,
      "isPartialFulfillment": false,
      "overallScore": 92.5,
      "locationAllocations": [
        {
          "locationId": 3,
          "locationName": "Manhattan Express Hub",
          "allocatedQuantity": 1,
          "locationScore": 92.5,
          "deliveryTiming": {
            "estimatedShipDate": "2024-01-15T13:00:00",
            "estimatedDeliveryDate": "2024-01-15T18:00:00",
            "transitTimeDays": 0,
            "processingTimeHours": 2
          }
        }
      ]
    }
  ]
}
```

### Configuration Benefits

1. **Distance Optimization**: High distance penalty (-5.0) ensures nearest location is always preferred
2. **Speed Priority**: Strong transit time (-20.0) and processing time (-15.0) weights
3. **Single Location Preference**: High split penalty (50.0 base) keeps orders together
4. **Tight Geographic Filter**: 30km radius limit ensures feasible same-day delivery
5. **Express Bonus**: +30.0 weight for express delivery capabilities

### Performance Characteristics

| Metric | Value | Purpose |
|--------|-------|---------|
| Distance Weight | -5.0 | **Heavily penalize distance** (nearest wins) |
| Transit Time Weight | -20.0 | Prefer locations with fastest transit |
| Processing Time Weight | -15.0 | Prefer locations with fast processing |
| Express Weight | +30.0 | Bonus for same-day capabilities |
| Split Penalty Base | 50.0 | Strong preference for single location |
| Distance Threshold | 30km | Maximum realistic same-day delivery radius |

This configuration ensures that for same-day delivery, the system will **always prioritize the nearest location** that can fulfill the order within the time constraints.

---

These examples demonstrate the full range of capabilities of the Order Sourcing Engine, from simple single-item requests to complex multi-item orders with various business requirements.