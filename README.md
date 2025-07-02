# Enhanced Order Sourcing Engine

## Overview

This project implements a sophisticated order sourcing workflow system designed for e-commerce order allocation. The main goal is to generate optimal fulfillment plans that efficiently allocate orders to the best available locations while meeting strict performance requirements (sub-50ms response time).

## Quick Start

### Running with H2 Database (Recommended for testing)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

This will start the application with an in-memory H2 database and automatically populate sample data.

### Running with PostgreSQL
1. Set up PostgreSQL database named `order_sourcing`
2. Update credentials in `application.yml`
3. Run: `mvn spring-boot:run`

### Database Changes
**Note**: This version has been updated to fetch inventory data directly from the database instead of external API calls, improving performance and reliability.

## Architecture

### Core Components

1. **Rule Engine**: Uses AviatorScript for flexible, expression-based rule evaluation
2. **Workflow Management**: Configurable workflows based on delivery types
3. **Scoring System**: Multi-factor scoring algorithm for optimal location selection
4. **Caching Layer**: Performance optimization through expression and result caching
5. **Fallback Strategies**: Robust handling of edge cases and inventory shortages

### Key Features

- **Sub-50ms Performance**: Optimized for high-speed order processing
- **Flexible Rule System**: Support for both simple and complex business rules
- **Multi-factor Scoring**: Considers transit time, processing time, inventory, proximity, and capacity
- **Partial Fulfillment**: Handles scenarios where full order quantity isn't available
- **Backorder Support**: Configurable backorder handling for out-of-stock items
- **Capacity Management**: Considers location capacity constraints
- **Geographic Optimization**: Distance-based scoring for proximity preferences

## Enhanced Features

### 1. Advanced Rule System

#### Condition-Based Rules
```java
// Example: Filter locations by multiple conditions
Condition condition1 = new Condition();
condition1.setParameter("location.transitTime");
condition1.setOperator("lessThanOrEquals");
condition1.setValue("2");

Condition condition2 = new Condition();
condition2.setParameter("location.country");
condition2.setOperator("equals");
condition2.setValue("USA");

Rule filterRule = new Rule();
filterRule.setName("US Locations with Fast Transit");
filterRule.setType("filter");
filterRule.setLogicalOperator("AND");
filterRule.setConditions(Arrays.asList(condition1, condition2));
```

#### Rule Priorities and Enablement
- Rules can be prioritized (higher priority = executed first)
- Rules can be enabled/disabled without deletion
- Support for both filter and scoring rule types

### 2. Enhanced Configuration

#### RuleSet Configuration Options
```yaml
# Example configuration
ruleSet:
  # Scoring weights
  transitTimeWeight: 0.5
  processingTimeWeight: 0.3
  inventoryWeight: 0.6
  capacityWeight: 0.3
  proximityWeight: 0.4
  splitShipmentPenalty: 0.2
  
  # Constraints
  minInventoryThreshold: 1
  maxCapacityUtilization: 90
  
  # Business rules
  allowBackorder: false
  allowPartialFulfillment: true
  preferSingleLocation: true
  
  # Performance settings
  maxLocationsToEvaluate: 50
  enableCaching: true
  enableParallelProcessing: false
  
  # Fallback strategy
  defaultFallbackStrategy: "NEAREST" # NEAREST, FASTEST, CHEAPEST
```

### 3. Performance Optimizations

#### Expression Caching
- Compiled AviatorScript expressions are cached for reuse
- Configurable cache expiration
- Thread-safe concurrent cache implementation

#### Location Limiting
- Configurable maximum number of locations to evaluate
- Early termination for performance-critical scenarios

#### Database Optimization
- Efficient inventory queries with quantity thresholds
- Indexed location and inventory lookups

## API Usage

### Basic Sourcing Request
```http
POST /api/sourcing/source?orderId=123
```

### Response Format
```json
[
  {
    "id": 1,
    "order": {
      "id": 123,
      "orderId": "ORDER123",
      "latitude": 42.3601,
      "longitude": -71.0589
    },
    "location": {
      "id": 1,
      "name": "Store 1",
      "transitTime": 2,
      "latitude": 42.3601,
      "longitude": -71.0589
    },
    "sku": "SKU123",
    "quantity": 2
  }
]
```

## Rule Engine Comparison

### Performance Analysis

| Engine | Simple Expression | Complex Expression | Compilation Time |
|--------|------------------|-------------------|------------------|
| AviatorScript | ~100 ns | ~500 ns | ~50 μs |
| Spring SpEL | ~200 ns | ~800 ns | ~100 μs |
| Native Java | ~10 ns | ~50 ns | 0 μs |

### Recommendations

1. **AviatorScript** (Current Choice)
   - ✅ Good balance of performance and flexibility
   - ✅ Rich expression language features
   - ✅ Suitable for complex business rules
   - ⚠️ Moderate compilation overhead

2. **Spring SpEL**
   - ✅ Native Spring integration
   - ✅ Good for simple expressions
   - ❌ Slower execution for complex rules
   - ❌ Higher compilation overhead

3. **Native Java**
   - ✅ Fastest execution
   - ❌ No runtime flexibility
   - ❌ Requires code changes for rule modifications

## Configuration Examples

### Express Delivery Workflow
```java
// High priority for speed, lower weight on cost
RuleSet expressRuleSet = new RuleSet();
expressRuleSet.setTransitTimeWeight(1.0);  // High weight on speed
expressRuleSet.setProximityWeight(0.8);    // Prefer nearby locations
expressRuleSet.setInventoryWeight(0.4);    // Lower inventory priority
expressRuleSet.setMaxLocationsToEvaluate(20); // Limit for speed
expressRuleSet.setDefaultFallbackStrategy("FASTEST");
```

### Cost-Optimized Workflow
```java
// Optimize for cost efficiency
RuleSet costOptimizedRuleSet = new RuleSet();
costOptimizedRuleSet.setTransitTimeWeight(0.2);  // Lower speed priority
costOptimizedRuleSet.setCostWeight(1.0);         // High cost priority
costOptimizedRuleSet.setInventoryWeight(0.8);    // Prefer high inventory
costOptimizedRuleSet.setPreferSingleLocation(true); // Reduce split shipments
```

### High-Volume Workflow
```java
// Optimized for high-volume processing
RuleSet highVolumeRuleSet = new RuleSet();
highVolumeRuleSet.setEnableCaching(true);
highVolumeRuleSet.setMaxLocationsToEvaluate(10); // Aggressive limiting
highVolumeRuleSet.setEnableParallelProcessing(true);
```

## Testing

### Performance Testing
```java
@Test
void testPerformanceRequirement() {
    // Warm up JVM
    for (int i = 0; i < 10; i++) {
        sourcingService.source(testOrder);
    }
    
    // Measure performance over 100 iterations
    long avgTimeMillis = measureAverageExecutionTime(100);
    
    // Assert sub-50ms requirement
    assertTrue(avgTimeMillis < 50, 
        "Sourcing should complete in under 50ms. Actual: " + avgTimeMillis + "ms");
}
```

### Functional Testing
- Basic sourcing scenarios
- Partial fulfillment handling
- Backorder processing
- Location filtering
- Scoring logic validation
- Edge case handling

## Deployment Considerations

### Database Setup
```sql
-- Recommended indexes for performance
CREATE INDEX idx_inventory_sku_quantity ON inventory(sku, quantity);
CREATE INDEX idx_location_coordinates ON location(latitude, longitude);
CREATE INDEX idx_workflow_delivery_type ON workflow(delivery_type);
```

### Application Properties
```yaml
spring:
  cache:
    type: concurrent
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
```

### Monitoring
- Track average response times
- Monitor cache hit rates
- Alert on response times > 45ms
- Monitor rule execution failures

## Future Enhancements

1. **Machine Learning Integration**
   - Predictive inventory management
   - Dynamic weight optimization
   - Historical performance learning

2. **Advanced Caching**
   - Redis integration for distributed caching
   - Intelligent cache warming
   - Cache invalidation strategies

3. **Real-time Inventory**
   - WebSocket-based inventory updates
   - Event-driven inventory synchronization
   - Conflict resolution for concurrent orders

4. **Geographic Optimization**
   - Route optimization integration
   - Traffic-aware transit time calculation
   - Multi-modal shipping options

## Contributing

1. Follow the existing code style and patterns
2. Add comprehensive tests for new features
3. Ensure performance requirements are maintained
4. Update documentation for configuration changes
5. Consider backward compatibility for API changes

## License

This project is licensed under the MIT License - see the LICENSE file for details.