# Advanced Routing Guide for Modern Retailers

## Overview

This guide explains the advanced routing capabilities implemented in the order sourcing engine to handle complex business scenarios for modern retailers. The system now supports sophisticated routing decisions for different delivery types, including Same Day Delivery and Ship from Store items.

## Business Problem

Modern retailers face complex routing decisions when orders contain items with different delivery types:

- **Same Day Delivery** items need to be fulfilled quickly from nearby locations
- **Ship from Store** items can be fulfilled from a broader network of store locations
- Sometimes business rules require **grouping** items to the same location for efficiency
- Other times business rules require **separating** items to different locations for operational reasons

## Solution Architecture

### Core Components

1. **RoutingStrategy**: Defines how different delivery type combinations should be handled
2. **LocationNetwork**: Groups locations by delivery type capabilities and constraints
3. **AdvancedRoutingService**: Implements sophisticated routing logic based on strategies
4. **Conditional Logic**: Uses AviatorScript expressions for dynamic routing decisions

### Key Features

- **Delivery-Type-Specific Location Filtering**: "If delivery type is X, choose 10 locations in network Y"
- **Intelligent Item Grouping/Separation**: Business rules determine when to group vs separate items
- **Conditional Routing**: Dynamic decisions based on order characteristics
- **Network-Based Location Management**: Different location sets for different delivery types

## Routing Strategies

### 1. GROUP Strategy
Groups items to the same location when beneficial.

```java
// Example: Group Same Day and Ship from Store items
RoutingStrategy groupingStrategy = new RoutingStrategy();
groupingStrategy.setStrategyType("GROUP");
groupingStrategy.setAllowCrossDeliveryTypeGrouping(true);
groupingStrategy.setPreferSameLocation(true);
```

**Use Cases:**
- Reduce shipping costs by consolidating shipments
- Minimize split shipments for customer convenience
- Optimize warehouse operations

### 2. SEPARATE Strategy
Forces items to be fulfilled from different locations.

```java
// Example: Separate Same Day and Ship from Store for business reasons
RoutingStrategy separationStrategy = new RoutingStrategy();
separationStrategy.setStrategyType("SEPARATE");
separationStrategy.setForceSeparateLocations(true);
separationStrategy.setSeparationReason("BUSINESS_RULE");
```

**Use Cases:**
- Compliance with business rules (e.g., fresh items from specific locations)
- Load balancing across locations
- Risk mitigation (avoid single point of failure)

### 3. CONDITIONAL Strategy
Makes routing decisions based on order characteristics.

```java
// Example: Conditional routing based on item count and delivery types
RoutingStrategy conditionalStrategy = new RoutingStrategy();
conditionalStrategy.setStrategyType("CONDITIONAL");
conditionalStrategy.setConditionalExpression("totalItems <= 3 && hasMultipleDeliveryTypes");
```

**Use Cases:**
- Small orders: group for efficiency
- Large orders: separate for capacity management
- High-value orders: use premium locations

## Location Networks

### Network Types

1. **Same Day Network**: Fast fulfillment locations
2. **Ship from Store Network**: Store-based fulfillment
3. **Standard Network**: Traditional distribution centers

### Network Configuration

```java
LocationNetwork sameDayNetwork = new LocationNetwork();
sameDayNetwork.setNetworkType("SAME_DAY");
sameDayNetwork.setSupportedDeliveryTypes(Arrays.asList("SAME_DAY"));
sameDayNetwork.setMaxTransitTime(1); // Same day constraint
sameDayNetwork.setMaxLocationsToEvaluate(10); // Performance optimization
```

## Business Scenarios

### Scenario 1: Group Same Day and Ship from Store Items

**Business Rule**: "When an order has both Same Day and Ship from Store items, try to fulfill from the same location if possible."

**Configuration**:
```java
RoutingStrategy strategy = new RoutingStrategy();
strategy.setApplicableDeliveryTypes(Arrays.asList("SAME_DAY", "SHIP_FROM_STORE"));
strategy.setStrategyType("GROUP");
strategy.setAllowCrossDeliveryTypeGrouping(true);
strategy.setAllowedLocationNetwork("SAME_DAY"); // Prefer same day network
```

**Result**: System finds locations that can handle both delivery types and groups items together.

### Scenario 2: Separate Items for Operational Reasons

**Business Rule**: "During peak seasons, separate Same Day and Ship from Store items to different locations to avoid capacity constraints."

**Configuration**:
```java
RoutingStrategy strategy = new RoutingStrategy();
strategy.setApplicableDeliveryTypes(Arrays.asList("SAME_DAY", "SHIP_FROM_STORE"));
strategy.setStrategyType("SEPARATE");
strategy.setForceSeparateLocations(true);
strategy.setSeparationReason("CAPACITY");
```

**Result**: System ensures items go to different locations, optimizing capacity utilization.

### Scenario 3: Conditional Routing Based on Order Size

**Business Rule**: "For small orders (≤3 items), group items together. For larger orders, separate them."

**Configuration**:
```java
RoutingStrategy strategy = new RoutingStrategy();
strategy.setStrategyType("CONDITIONAL");
strategy.setConditionalExpression("totalItems <= 3");
// If true: group, if false: separate
```

**Result**: Dynamic routing decisions based on order characteristics.

## API Usage

### Basic Advanced Routing

```http
POST /api/sourcing/source-advanced?orderId=123
```

### Response Format

```json
{
  "fulfillmentPlans": [
    {
      "id": 1,
      "order": {"id": 123, "orderId": "ORDER_MIXED_DELIVERY"},
      "location": {"id": 1, "name": "Store 1"},
      "sku": "SKU123",
      "quantity": 1
    }
  ],
  "routingType": "ADVANCED",
  "totalPlans": 1,
  "message": "Order sourced using advanced routing strategies"
}
```

## Configuration Examples

### E-commerce Retailer Configuration

```java
// Peak season: separate items to balance load
RoutingStrategy peakSeason = new RoutingStrategy();
peakSeason.setName("Peak Season Load Balancing");
peakSeason.setStrategyType("SEPARATE");
peakSeason.setPriority(10);
peakSeason.setEnabled(true); // Enable during peak season

// Regular season: group for efficiency
RoutingStrategy regularSeason = new RoutingStrategy();
regularSeason.setName("Regular Season Grouping");
regularSeason.setStrategyType("GROUP");
regularSeason.setPriority(5);
regularSeason.setEnabled(true);
```

### Grocery Retailer Configuration

```java
// Fresh items: use specific network
RoutingStrategy freshItems = new RoutingStrategy();
freshItems.setAllowedLocationNetwork("FRESH_NETWORK");
freshItems.setMaxLocationsPerDeliveryType(5);
freshItems.setStrategyType("SEPARATE"); // Separate fresh from dry goods
```

## Performance Considerations

### Location Limiting
```java
strategy.setMaxLocationsPerDeliveryType(10); // Limit evaluation for performance
```

### Expression Caching
```java
// Conditional expressions are automatically cached for performance
strategy.setConditionalExpression("totalItems > 5 && hasSameDayDelivery");
```

### Network Prioritization
```java
network.setPriorityLevel(10); // Higher priority networks evaluated first
```

## Testing Scenarios

### Test Data Setup

The system includes sample data for testing:

1. **ORDER_MIXED_DELIVERY**: Contains both SAME_DAY and SHIP_FROM_STORE items
2. **ORDER_MULTIPLE_SAME_DAY**: Contains multiple same day items
3. **Multiple routing strategies**: GROUP, SEPARATE, and CONDITIONAL examples

### Testing Different Strategies

1. **Enable Grouping Strategy**:
   ```sql
   UPDATE routing_strategy SET enabled = true WHERE strategy_type = 'GROUP';
   UPDATE routing_strategy SET enabled = false WHERE strategy_type = 'SEPARATE';
   ```

2. **Test with Mixed Order**:
   ```http
   POST /api/sourcing/source-advanced?orderId=2
   ```

3. **Enable Separation Strategy**:
   ```sql
   UPDATE routing_strategy SET enabled = false WHERE strategy_type = 'GROUP';
   UPDATE routing_strategy SET enabled = true WHERE strategy_type = 'SEPARATE';
   ```

4. **Test Again**:
   ```http
   POST /api/sourcing/source-advanced?orderId=2
   ```

## Monitoring and Analytics

### Key Metrics

- **Grouping Rate**: Percentage of orders with items grouped to same location
- **Separation Rate**: Percentage of orders with items separated
- **Strategy Effectiveness**: Performance metrics per routing strategy
- **Network Utilization**: Usage statistics per location network

### Logging

The system provides detailed logging:
```
Using routing strategy: Group Same Day and Ship from Store (Type: GROUP)
Falling back to enhanced sourcing service for order: ORDER123
```

## Best Practices

### Strategy Design

1. **Use Priority Levels**: Higher priority strategies override lower ones
2. **Enable/Disable Flexibility**: Use enabled flag for seasonal adjustments
3. **Network Alignment**: Align routing strategies with location networks
4. **Performance Limits**: Set reasonable location evaluation limits

### Conditional Expressions

1. **Keep Simple**: Complex expressions impact performance
2. **Use Caching**: Expressions are automatically cached
3. **Test Thoroughly**: Validate expressions with various order types
4. **Document Logic**: Clear naming and descriptions

### Network Management

1. **Regular Updates**: Keep location networks current
2. **Capacity Monitoring**: Monitor network utilization
3. **Geographic Constraints**: Use geographic filters appropriately
4. **Service Level Alignment**: Match networks to delivery requirements

## Troubleshooting

### Common Issues

1. **No Strategy Found**: Check strategy applicability and enabled status
2. **No Locations Available**: Verify network configuration and location assignments
3. **Performance Issues**: Review location limits and expression complexity
4. **Unexpected Routing**: Check strategy priority and conditional logic

### Debug Information

Enable debug logging to see:
- Strategy selection process
- Location filtering results
- Conditional expression evaluation
- Fallback scenarios

## Future Enhancements

### Planned Features

1. **Machine Learning Integration**: Learn optimal routing patterns
2. **Real-time Capacity**: Dynamic capacity-based routing
3. **Cost Optimization**: Include shipping cost in routing decisions
4. **Customer Preferences**: Consider customer delivery preferences

### Extensibility

The system is designed for easy extension:
- Add new strategy types
- Create custom conditional expressions
- Implement new network types
- Add additional scoring factors

## Conclusion

The advanced routing system provides modern retailers with the flexibility to handle complex business scenarios while maintaining performance and scalability. By combining routing strategies, location networks, and conditional logic, businesses can optimize their fulfillment operations for various delivery types and operational requirements.