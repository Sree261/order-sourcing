# Order Sourcing Engine - Technical Flow Documentation

## Overview

This document provides a detailed technical walkthrough of how the Order Sourcing Engine processes requests from the moment an API call is received until the response is returned. It traces through every service method invocation, data transformation, and decision point.

## Application Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────────┐
│  SourcingController │───▶│ BatchSourcingService │───▶│ LocationFilterService │
└─────────────────┘    └──────────────────┘    └─────────────────────┘
                              │                           │
                              ▼                           ▼
                     ┌──────────────────┐         ┌─────────────────┐
                     │ InventoryService │         │ AviatorScript   │
                     └──────────────────┘         │ Engine          │
                              │                   └─────────────────┘
                              ▼
                     ┌──────────────────┐
                     │ PromiseDateService│───┐
                     └──────────────────┘   │
                              │              │
                              ▼              ▼
                     ┌──────────────────┐ ┌───────────────┐
                     │ ScoringService   │ │ CarrierService│
                     └──────────────────┘ └───────────────┘
```

## Complete Request Flow

### 1. API Entry Point

**File:** `SourcingController.java`
**Method:** `sourceOrder(@RequestBody OrderDTO orderDTO)`

```java
@PostMapping("/source")
public ResponseEntity<SourcingResponse> sourceOrder(@RequestBody @Valid OrderDTO orderDTO)
```

**What happens:**
1. **Request Validation**: Spring Boot validates the incoming JSON against the `OrderDTO` structure
2. **Logging**: Logs the incoming request with order ID and item count
3. **Location Filter Validation**: Validates each order item has a `locationFilterId`
   ```java
   for (OrderItemDTO item : orderDTO.getOrderItems()) {
       if (item.getLocationFilterId() == null || item.getLocationFilterId().trim().isEmpty()) {
           return ResponseEntity.badRequest().body(createErrorSourcingResponse(...));
       }
   }
   ```
4. **Service Delegation**: Calls `batchSourcingService.sourceOrder(orderDTO)`
5. **Response Building**: Wraps the service response in HTTP ResponseEntity
6. **Error Handling**: Catches exceptions and returns appropriate error responses

**Key Decision Points:**
- Validates required fields before processing
- Returns HTTP 400 for validation failures
- Returns HTTP 500 for internal errors

---

### 2. Core Orchestration Service

**File:** `BatchSourcingServiceImpl.java`
**Method:** `sourceOrder(OrderDTO order)`

```java
public SourcingResponse sourceOrder(OrderDTO order)
```

**What happens:**
1. **Performance Tracking**: Records start time for processing metrics
2. **Strategy Decision**: Calls `decideSourcingStrategy(order)` to determine processing approach
3. **Strategy Execution**: Based on strategy, calls either:
   - `batchSourceOrder(order)` for complex orders
   - `sequentialSourceOrder(order)` for simple orders
4. **Response Assembly**: Builds final `SourcingResponse` with processing time
5. **Error Handling**: Creates error response if any step fails

#### 2.1 Strategy Decision Logic

**Method:** `decideSourcingStrategy(OrderDTO order)`

**Decision Tree:**
```java
if (order.getOrderItems().size() >= BATCH_THRESHOLD_ITEMS ||          // ≥3 items
    totalQuantity >= BATCH_THRESHOLD_TOTAL_QUANTITY ||                 // ≥10 total units
    hasMultipleDeliveryTypes(order)) {                                 // Mixed delivery types
    return SourcingStrategy.BATCH;
} else {
    return SourcingStrategy.SEQUENTIAL;
}
```

**Key Constants:**
- `BATCH_THRESHOLD_ITEMS = 3`
- `BATCH_THRESHOLD_TOTAL_QUANTITY = 10`

---

### 3A. Sequential Processing Path (Simple Orders)

**Method:** `sequentialSourceOrder(OrderDTO order)`

**Used for:** Single items, simple orders, PDP requests
**Expected Performance:** 15-40ms

**Flow for each OrderItem:**

#### Step 1: Location Filtering
```java
List<Location> locations = locationFilterService.executeLocationFilter(
    orderItem.getLocationFilterId(), order);
```

**What happens in LocationFilterExecutionService:**
1. **Cache Check**: Looks for pre-computed results for the filter
2. **Filter Retrieval**: Gets the `LocationFilter` from database by ID
3. **Script Execution**: Executes AviatorScript filter on all locations
4. **Results**: Returns list of locations that pass the filter criteria

#### Step 2: Inventory Fetching
```java
List<Inventory> inventories = inventoryApiService.fetchInventoryBySku(orderItem.getSku());
```

**What happens in InventoryApiService:**
1. **Database Query**: Queries inventory table for the specific SKU
2. **Filtering**: Filters results to only include locations from step 1
3. **Availability Check**: Ensures available quantity > 0
4. **Results**: Returns inventory records for eligible locations

#### Step 3: Fulfillment Strategy Evaluation
```java
FulfillmentStrategy strategy = findOptimalFulfillmentStrategy(locations, inventories, orderItem, order);
```

**What happens in findOptimalFulfillmentStrategy:**
1. **Location Scoring**: For each location-inventory pair:
   ```java
   double score = calculateLocationScore(location, inventory, orderItem, order);
   ```
2. **Single Location Evaluation**: Calls `evaluateSingleLocationStrategy()`
   - Checks if any single location can fulfill complete order
   - Considers business rules (requiresFullQuantity, isPartialFulfillmentAllowed)
3. **Multi-Location Evaluation**: If single location fails, calls `evaluateMultiLocationStrategy()`
   - Distributes quantity across multiple locations
   - Applies split penalties using `calculateSplitPenalty()`
4. **Strategy Selection**: Returns best strategy (single vs multi-location)

#### Step 4: Promise Date Calculation
```java
PromiseDateBreakdown promiseDate = promiseDateService.calculateEnhancedPromiseDate(
    orderItem, primaryLocation, primaryInventory, order);
```

**What happens in PromiseDateService:**
1. **Carrier Selection**: Calls `carrierService.getBestCarrierConfiguration()`
2. **Transit Time Calculation**: Based on delivery type and distance
3. **Processing Time**: From inventory processing hours
4. **Business Rules**: Adjusts for weekends, holidays, cutoff times
5. **Results**: Returns complete promise date breakdown

#### Step 5: Fulfillment Plan Building
```java
SourcingResponse.FulfillmentPlan plan = buildFulfillmentPlan(orderItem, strategy, promiseDate, order);
```

**What happens in buildFulfillmentPlan:**
1. **Location Allocation Creation**: For each allocated location:
   ```java
   SourcingResponse.LocationAllocation allocation = SourcingResponse.LocationAllocation.builder()
       .locationId(location.getId())
       .locationName(location.getName())
       .allocatedQuantity(pair.allocatedQuantity)
       .locationScore(pair.score)
       .deliveryTiming(deliveryTiming)
       .build();
   ```
2. **Delivery Timing**: Converts PromiseDateBreakdown to DeliveryTiming
3. **Plan Assembly**: Creates complete FulfillmentPlan with all details

---

### 3B. Batch Processing Path (Complex Orders)

**Method:** `batchSourceOrder(OrderDTO order)`

**Used for:** Multiple items, large quantities, mixed delivery types
**Expected Performance:** 80-150ms

**Optimizations:**
- Parallel processing of filters and inventory
- Deduplication of identical filter executions
- Bulk operations where possible

#### Step 1: Filter Grouping and Parallel Execution
```java
Map<String, List<OrderItemDTO>> filterGroups = order.getOrderItems().stream()
    .collect(Collectors.groupingBy(OrderItemDTO::getLocationFilterId));

CompletableFuture<Map<String, List<Location>>> filterFuture = 
    locationFilterService.batchExecuteFilters(filterGroups.keySet(), order);
```

**What happens in batchExecuteFilters:**
1. **Parallel Stream**: Executes filters in parallel using `parallelStream()`
2. **Deduplication**: Each unique filter ID executed only once
3. **Concurrent Results**: Returns Map of filterId → List<Location>

#### Step 2: Parallel Inventory Fetching
```java
CompletableFuture<Map<String, List<Inventory>>> inventoryFuture = 
    inventoryApiService.batchFetchInventory(order.getOrderItems());
```

**What happens in batchFetchInventory:**
1. **SKU Collection**: Extracts unique SKUs from all order items
2. **Bulk Database Query**: Single query for all SKUs
3. **Result Grouping**: Groups results by SKU for easy lookup

#### Step 3: Parallel Promise Date Calculation
```java
CompletableFuture<Map<String, PromiseDateBreakdown>> promiseFuture = 
    promiseDateService.batchCalculatePromiseDates(
        order.getOrderItems(), filterResults, inventoryResults, order);
```

**What happens in batchCalculatePromiseDates:**
1. **Parallel Processing**: Calculates promise dates for all items in parallel
2. **Carrier Optimization**: Reuses carrier configurations where possible
3. **Bulk Results**: Returns Map of itemKey → PromiseDateBreakdown

#### Step 4: Results Assembly
```java
List<SourcingResponse.FulfillmentPlan> fulfillmentPlans = 
    buildFulfillmentPlans(order.getOrderItems(), filterResults, inventoryResults, promiseResults, order);
```

**What happens in buildFulfillmentPlans:**
1. **Item Processing**: For each order item, combines all the parallel results
2. **Strategy Evaluation**: Same logic as sequential, but with pre-fetched data
3. **Plan Building**: Creates fulfillment plans using cached results

---

### 4. Location Scoring Deep Dive

**File:** `BatchSourcingServiceImpl.java`
**Method:** `calculateLocationScore(Location location, Inventory inventory, OrderItemDTO orderItem, OrderDTO order)`

**Scoring Formula Implementation:**
```java
// Get scoring configuration for this item
ScoringConfiguration config = scoringConfigurationService.getScoringConfigurationForItem(orderItem);

// Calculate base score components
double score = scoringConfigurationService.calculateLocationScore(location, config, orderItem, context);
```

**What happens in ScoringConfigurationService.calculateLocationScore:**

1. **Component Calculations:**
   ```java
   // Transit time component (negative = faster is better)
   double transitScore = config.getTransitTimeWeight() * location.getTransitTimeDays();
   
   // Processing time component (negative = faster is better)  
   double processingScore = config.getProcessingTimeWeight() * inventory.getProcessingTimeHours();
   
   // Inventory availability component (positive = more inventory is better)
   double inventoryRatio = Math.min(1.0, (double) inventory.getQuantity() / orderItem.getQuantity());
   double inventoryScore = config.getInventoryWeight() * inventoryRatio;
   
   // Distance component (negative = closer is better)
   double distance = calculateDistance(location, order);
   double distanceScore = config.getDistanceWeight() * distance;
   
   // Express priority bonus
   double expressScore = orderItem.isExpressPriority() ? config.getExpressWeight() : 0.0;
   ```

2. **Score Combination:**
   ```java
   double totalScore = transitScore + processingScore + inventoryScore + 
                      distanceScore + expressScore;
   ```

3. **Penalty Applications:**
   - Split penalties for multi-location strategies
   - High-value item penalties
   - Special handling adjustments

---

### 5. Location Filtering with AviatorScript

**File:** `LocationFilterExecutionServiceImpl.java`
**Method:** `executeLocationFilter(String filterId, OrderDTO orderContext)`

#### Step 1: Filter Retrieval and Compilation
```java
LocationFilter filter = locationFilterRepository.findByIdAndIsActiveTrue(filterId);
Expression compiledExpression = getCompiledExpression(filter);
```

**What happens in getCompiledExpression:**
1. **Cache Check**: Looks in expression cache first
2. **Compilation**: If not cached, compiles AviatorScript using `AviatorEvaluator.compile()`
3. **Caching**: Stores compiled expression for reuse

#### Step 2: Context Creation for Each Location
```java
for (Location location : allLocations) {
    Map<String, Object> env = createExecutionEnvironment(location, orderContext, null);
    Boolean result = (Boolean) compiledExpression.execute(env);
}
```

**What happens in createExecutionEnvironment:**
1. **Location Context**: Adds location object with all properties
2. **Order Context**: Adds order details (customer location, items, etc.)
3. **Time Context**: Adds current time, business hours, weekend flags
4. **Distance Utilities**: Adds distance calculation functions
5. **Business Context**: Adds peak season, holiday flags
6. **Scoring Context**: Adds scoring weights for use in scripts
7. **Math Utilities**: Adds mathematical functions

**Example Environment Variables:**
```java
env.put("location", location);                    // Location object
env.put("order", orderContext);                   // Order details
env.put("time", timeContext);                     // Current time context
env.put("distance", new DistanceUtilities(...));  // Distance functions
env.put("business", new BusinessContext());       // Business rules
env.put("scoring", scoringWeights);               // Scoring configuration
env.put("math", new MathUtilities());             // Math functions
```

#### Step 3: Script Execution Examples

**Standard Delivery Filter:**
```javascript
// Script: location.transitTimeDays <= 5 && distance.calculate(location.latitude, location.longitude) <= 500
// Execution:
Boolean result = location.getTransitTimeDays() <= 5 && 
                distanceUtil.calculate(location.getLatitude(), location.getLongitude()) <= 500;
```

**Same-Day Delivery Filter:**
```javascript
// Script: location.transitTimeDays <= 1 && time.hour <= 14 && !time.isWeekend
// Execution:
Boolean result = location.getTransitTimeDays() <= 1 && 
                timeContext.get("hour") <= 14 && 
                !timeContext.get("isWeekend");
```

---

### 6. Promise Date Calculation Details

**File:** `PromiseDateServiceImpl.java`
**Method:** `calculateEnhancedPromiseDate(OrderItemDTO orderItem, Location location, Inventory inventory, OrderDTO orderContext)`

#### Step 1: Carrier Configuration
```java
Optional<CarrierConfiguration> carrierOpt = carrierService.getBestCarrierConfiguration(
    orderItem.getDeliveryType(), distance, orderItem);
```

**What happens in CarrierService.getBestCarrierConfiguration:**
1. **Carrier Query**: Gets all carriers for delivery type
2. **Suitability Check**: Calls `isCarrierSuitableForItem()` for each carrier
3. **Selection Logic**: Chooses best carrier based on:
   - Distance coverage
   - Delivery type support
   - Item category compatibility
   - Service level requirements

#### Step 2: Date Calculations
```java
LocalDateTime currentTime = LocalDateTime.now();
LocalDateTime estimatedShipDate = currentTime.plusHours(inventory.getProcessingTimeHours());
LocalDateTime estimatedDeliveryDate = estimatedShipDate.plusDays(location.getTransitTimeDays());
```

#### Step 3: Business Rule Adjustments
1. **Same-Day Rules**: Must order before 2 PM
2. **Weekend Handling**: Skip weekends for business delivery
3. **Holiday Adjustments**: Account for shipping holidays
4. **Carrier Cutoffs**: Respect carrier pickup schedules

#### Step 4: Promise Date Assembly
```java
return PromiseDateBreakdown.builder()
    .promiseDate(finalDeliveryDate)
    .estimatedShipDate(estimatedShipDate)
    .estimatedDeliveryDate(estimatedDeliveryDate)
    .transitTimeDays(location.getTransitTimeDays())
    .processingTimeHours(inventory.getProcessingTimeHours())
    .build();
```

---

### 7. Data Flow Summary

```
Request → Controller Validation → Strategy Decision
                                        ↓
                          ┌─────────────────────────────┐
                          │                             │
                          ▼                             ▼
                  Sequential Processing          Batch Processing
                          │                             │
                          ▼                             ▼
              ┌─ Location Filtering                Parallel:
              ├─ Inventory Fetching                ├─ Batch Location Filtering
              ├─ Strategy Evaluation               ├─ Batch Inventory Fetching  
              ├─ Promise Date Calc                 ├─ Batch Promise Date Calc
              └─ Plan Building                     └─ Results Assembly
                          │                             │
                          └──────────────┬──────────────┘
                                         ▼
                               Response Assembly → Controller → HTTP Response
```

## Performance Characteristics

### Sequential Processing
- **Target**: 15-40ms
- **Optimization**: Direct processing, minimal overhead
- **Cache Usage**: Expression cache, scoring cache
- **Database Queries**: 2-3 per item (filter lookup, inventory, scoring config)

### Batch Processing  
- **Target**: 80-150ms for 3-10 items
- **Optimization**: Parallel execution, bulk operations
- **Cache Usage**: All caches plus result deduplication
- **Database Queries**: Bulk queries reduce total round trips

## Error Handling Strategy

### Controller Level
- Validation errors → HTTP 400
- Business logic errors → HTTP 400 with message
- System errors → HTTP 500

### Service Level
- Missing data → Empty results
- Invalid filters → Skip problematic items
- Calculation errors → Log and continue with defaults

### Resilience Patterns
- Null-safe operations throughout
- Graceful degradation when services fail
- Comprehensive logging for debugging

## Key Design Decisions

1. **Strategy-based Processing**: Automatic selection optimizes for order complexity
2. **AviatorScript Integration**: Provides flexible, high-performance location filtering
3. **Parallel Batch Processing**: Maximizes throughput for complex orders
4. **Comprehensive Caching**: Reduces database load and improves response times
5. **Modular Service Design**: Each service has single responsibility
6. **Immutable DTOs**: Thread-safe data structures prevent concurrency issues

This technical flow provides complete transparency into how the Order Sourcing Engine processes requests, making it easier for new developers to understand, debug, and extend the system.