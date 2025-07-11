# Order Sourcing Engine - Implementation Guide

## Architecture Overview

The Order Sourcing Engine uses a modular architecture with intelligent processing strategies to optimize order fulfillment decisions. The system automatically chooses between batch and sequential processing based on order complexity.

### Core Components

1. **SourcingController** - Single REST endpoint `/api/sourcing/source`
2. **BatchSourcingService** - Core orchestration with intelligent strategy selection
3. **LocationFilterExecutionService** - Dynamic location filtering using AviatorScript
4. **InventoryApiService** - Inventory data retrieval and management
5. **PromiseDateService** - Delivery promise date calculations
6. **ScoringConfigurationService** - Location scoring and penalty calculations
7. **CarrierService** - Carrier configuration and routing logic

## Processing Strategies

### Automatic Strategy Selection

The engine automatically decides between two processing strategies:

```java
// Batch processing triggers when:
- Number of items ≥ 3
- Total quantity ≥ 10 units
- Multiple delivery types present
- Order complexity score > threshold
```

**Sequential Processing** (Single items, simple orders):
- Processes each item individually
- Optimized for speed (15-40ms)
- Direct location filtering and scoring

**Batch Processing** (Complex orders, multiple items):
- Parallel filter execution
- Bulk inventory fetching
- Optimized allocation algorithms
- Handles 3+ items efficiently (80-150ms)

## Location Scoring Algorithm

### Core Scoring Formula

```java
score = (transitTimeWeight × location.transitTime) +
        (processingTimeWeight × inventory.processingTime) +
        (inventoryWeight × inventory.availabilityRatio) +
        (distanceWeight × distanceFromCustomer) +
        (expressWeight × isExpressPriority) -
        (splitPenalty × multiLocationFactor)
```

### Scoring Configuration

The system supports multiple scoring configurations:

| Configuration | Use Case | Key Weights |
|--------------|----------|-------------|
| DEFAULT_SCORING | Standard fulfillment | Balanced weights |
| ELECTRONICS_PREMIUM | High-value electronics | Security +20%, Distance -0.8 |
| EXPRESS_DELIVERY | Same/next-day delivery | Express +30%, Transit -15 |
| HAZMAT_SCORING | Hazardous materials | Compliance +25%, Special handling |

### Example Scoring Configuration

```sql
INSERT INTO scoring_configuration (
    id, name, description, is_active,
    transit_time_weight, processing_time_weight, inventory_weight,
    express_weight, distance_weight, split_penalty_base
) VALUES (
    'ELECTRONICS_PREMIUM_SCORING',
    'Electronics Premium',
    'High-value electronics with security requirements',
    true,
    -12.0,  -- Faster transit preferred
    -8.0,   -- Faster processing preferred  
    60.0,   -- High inventory availability weight
    25.0,   -- Express priority bonus
    -0.8,   -- Distance penalty
    20.0    -- Higher split penalty (prefer single location)
);
```

## AviatorScript Integration

### Overview

AviatorScript is a high-performance expression language used for dynamic location filtering. It provides:

- **Runtime expression compilation** for fast execution
- **Rich context variables** (location, order, time, business context)
- **Custom functions** for distance calculations and business logic
- **Caching** for compiled expressions

### Script Context Variables

```javascript
// Available in all AviatorScript expressions:

location {
    id, name, latitude, longitude,
    transitTimeDays, processingTimeHours,
    capabilities, type
}

order {
    tempOrderId, latitude, longitude,
    orderItems[], customerId, orderType,
    isPeakSeason, allowPartialShipments
}

time {
    hour, dayOfWeek, month,
    isWeekend, isBusinessHours
}

distance {
    calculate(lat, lon) -> distance in km
    isWithinRadius(lat, lon, radius) -> boolean
}

business {
    isPeakSeason() -> boolean
    isHoliday() -> boolean
    getCurrentTimeZone() -> string
}

scoring {
    transitTimeWeight, processingTimeWeight,
    inventoryWeight, expressWeight,
    // ... all scoring configuration values
}

math {
    sqrt(x), pow(x,y), abs(x), min(x,y), max(x,y)
}
```

### Example Filter Scripts

#### 1. Standard Delivery Filter
```javascript
// STANDARD_DELIVERY_RULE
location.transitTimeDays <= 5 && 
location.processingTimeHours <= 48 &&
distance.calculate(location.latitude, location.longitude) <= 500
```

#### 2. Same-Day Delivery Filter
```javascript
// SDD_FILTER_RULE  
location.transitTimeDays <= 1 &&
location.processingTimeHours <= 4 &&
distance.isWithinRadius(location.latitude, location.longitude, 50) &&
time.hour <= 14 &&  // Order by 2 PM for same-day
!time.isWeekend
```

#### 3. Electronics Security Filter
```javascript
// ELECTRONICS_SECURE_RULE
location.capabilities != nil && 
location.capabilities.contains("HIGH_SECURITY") &&
location.capabilities.contains("ELECTRONICS_CERTIFIED") &&
location.type == "WAREHOUSE" &&
distance.calculate(location.latitude, location.longitude) <= 200
```

#### 4. Peak Season Filter
```javascript
// PEAK_SEASON_RULE
if (business.isPeakSeason()) {
    location.processingTimeHours <= 24 &&
    location.transitTimeDays <= 3 &&
    location.capabilities.contains("PEAK_CAPACITY")
} else {
    location.processingTimeHours <= 48 &&
    location.transitTimeDays <= 5
}
```

#### 5. Hazmat Filter
```javascript
// HAZMAT_FILTER_RULE
location.capabilities.contains("HAZMAT_CERTIFIED") &&
location.capabilities.contains("GROUND_SHIPPING") &&
location.type == "WAREHOUSE" &&
!location.capabilities.contains("AIR_RESTRICTED")
```

## Business Use Case Configurations

### Use Case 1: E-commerce Fashion Retailer

**Business Requirements:**
- Fast fulfillment for premium customers
- Cost optimization for standard orders
- Seasonal capacity management
- Multi-location split minimization

**Configuration:**

```sql
-- Scoring Configuration
INSERT INTO scoring_configuration VALUES (
    'FASHION_PREMIUM_SCORING',
    'Fashion Premium',
    'Premium fashion with fast fulfillment',
    true,
    -10.0,  -- Transit time weight
    -6.0,   -- Processing time weight
    50.0,   -- Inventory weight
    20.0,   -- Express weight
    -0.5,   -- Distance weight
    25.0,   -- Split penalty base
    1.8,    -- Split penalty exponent
    15.0    -- Split penalty multiplier
);

-- Location Filter for Premium Customers
INSERT INTO location_filter VALUES (
    'FASHION_PREMIUM_RULE',
    'Fashion Premium Customer Filter',
    'Fast fulfillment for premium fashion customers',
    'location.transitTimeDays <= 2 && 
     location.processingTimeHours <= 24 && 
     distance.calculate(location.latitude, location.longitude) <= 300 &&
     (location.capabilities.contains("EXPRESS_FASHION") || 
      location.capabilities.contains("PREMIUM_HANDLING"))',
    true,
    1,
    60  -- Cache for 1 hour
);
```

**API Usage:**
```json
{
  "tempOrderId": "FASHION_001",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "orderItems": [
    {
      "sku": "DRESS_001",
      "quantity": 1,
      "deliveryType": "NEXT_DAY",
      "locationFilterId": "FASHION_PREMIUM_RULE",
      "scoringConfigurationId": "FASHION_PREMIUM_SCORING",
      "productCategory": "FASHION_APPAREL"
    }
  ],
  "customerId": "PREMIUM_CUST_001",
  "customerTier": "PREMIUM",
  "orderType": "WEB"
}
```

### Use Case 2: Electronics B2B Distributor

**Business Requirements:**
- High-security handling for valuable items
- Bulk order optimization
- Carrier restrictions for certain products
- Signature requirements

**Configuration:**

```sql
-- Scoring Configuration  
INSERT INTO scoring_configuration VALUES (
    'B2B_ELECTRONICS_SCORING',
    'B2B Electronics',
    'Bulk electronics distribution with security',
    true,
    -8.0,   -- Transit time weight
    -5.0,   -- Processing time weight  
    70.0,   -- High inventory weight for bulk
    15.0,   -- Express weight
    -0.3,   -- Distance weight (less important for B2B)
    30.0,   -- Higher split penalty (prefer consolidation)
    2.0,    -- Split penalty exponent
    20.0    -- Split penalty multiplier
);

-- Location Filter for Electronics
INSERT INTO location_filter VALUES (
    'B2B_ELECTRONICS_RULE',
    'B2B Electronics Security Filter',
    'High-security locations for electronics distribution',
    'location.capabilities.contains("HIGH_SECURITY") &&
     location.capabilities.contains("ELECTRONICS_CERTIFIED") &&
     location.capabilities.contains("B2B_SHIPPING") &&
     location.type == "WAREHOUSE" &&
     distance.calculate(location.latitude, location.longitude) <= 1000',
    true,
    1,
    120  -- Cache for 2 hours
);
```

**API Usage:**
```json
{
  "tempOrderId": "B2B_BULK_001",
  "latitude": 39.9526,
  "longitude": -75.1652,
  "orderItems": [
    {
      "sku": "LAPTOP_PRO_001",
      "quantity": 50,
      "deliveryType": "STANDARD",
      "locationFilterId": "B2B_ELECTRONICS_RULE", 
      "scoringConfigurationId": "B2B_ELECTRONICS_SCORING",
      "productCategory": "ELECTRONICS_COMPUTER",
      "requiresSignature": true,
      "specialHandling": "HIGH_VALUE"
    }
  ],
  "customerId": "B2B_CORP_001",
  "customerTier": "ENTERPRISE",
  "allowPartialShipments": true,
  "preferSingleLocation": false,
  "orderType": "B2B"
}
```

## Calculation Details

### Distance Calculation
```java
// Simplified distance calculation (for demo purposes)
double distance = Math.sqrt(
    Math.pow(location.latitude - customer.latitude, 2) + 
    Math.pow(location.longitude - customer.longitude, 2)
) * 111.32; // Convert to approximate kilometers
```

### Promise Date Calculation
```java
LocalDateTime promiseDate = orderTime
    .plusHours(inventory.processingTimeHours)
    .plusDays(location.transitTimeDays)
    .plus(carrierConfiguration.serviceLevelAdjustment);

// Adjust for business days, holidays, carrier cutoffs
if (deliveryType.equals("SAME_DAY")) {
    // Must be ordered before 2 PM for same-day delivery
    promiseDate = adjustForSameDayRules(promiseDate, orderTime);
}
```

### Split Penalty Calculation
```java
double splitPenalty = config.splitPenaltyBase * 
                     Math.pow(locationCount, config.splitPenaltyExponent) *
                     config.splitPenaltyMultiplier;

// Apply to overall score
double finalScore = baseScore - splitPenalty;
```

## Performance Optimizations

### Caching Strategy
- **Expression Cache**: Compiled AviatorScript expressions
- **Location Filter Cache**: Filtered results (TTL: 30-120 minutes)
- **Scoring Configuration Cache**: Weights and rules
- **Inventory Cache**: Frequently accessed data

### Batch Processing Benefits
- **Parallel Execution**: Location filtering and inventory fetching
- **Deduplication**: Eliminate duplicate filter executions
- **Connection Pooling**: Efficient database usage
- **Result Streaming**: Process as results arrive

## Monitoring and Debugging

### Key Metrics to Monitor
- Response time per strategy (sequential vs batch)
- Cache hit rates for filters and scoring
- Location evaluation counts
- Partial fulfillment rates
- Split shipment frequency

### Debug Logging
```yaml
logging:
  level:
    com.ordersourcing.engine.service: DEBUG
    com.ordersourcing.engine.controller: INFO
```

## Customization Guidelines

1. **Scoring Weights**: Adjust based on business priorities
2. **Filter Scripts**: Create location-specific business rules
3. **Cache TTL**: Balance performance vs data freshness
4. **Batch Thresholds**: Tune based on order patterns
5. **Penalty Factors**: Optimize for cost vs customer experience