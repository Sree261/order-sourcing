# Business Configuration Guide: Order Sourcing Engine

## Overview

The Order Sourcing Engine provides a powerful **Configurable Scoring System** that allows businesses to dynamically adjust how locations are selected for order fulfillment. This guide explains how to customize the order routing process to optimize for different business scenarios, product types, and market conditions.

## 🎯 Key Business Benefits

### 1. **Dynamic Business Rule Management**
- **No Code Deployments Required**: Change scoring logic instantly through database configuration
- **Real-time Adaptation**: Respond to market changes, seasonal demands, or operational constraints immediately
- **A/B Testing Capability**: Test different scoring strategies simultaneously to optimize performance

### 2. **Product-Specific Optimization**
- **Category-Based Scoring**: Different rules for electronics, groceries, hazmat, high-value items
- **Custom Weightings**: Tailor location selection criteria for specific product characteristics
- **Compliance Integration**: Built-in support for regulatory requirements (hazmat, cold chain, etc.)

### 3. **Operational Flexibility**
- **Peak Season Adjustments**: Automatically adjust for capacity constraints during high-demand periods
- **Geographic Optimization**: Fine-tune location selection based on regional performance
- **Carrier Integration**: Optimize routing based on carrier performance and costs

## 🔧 Scoring Configuration System

### Core Scoring Factors

The system evaluates locations based on these configurable factors:

| Factor | Description | Weight Range | Business Impact |
|--------|-------------|--------------|-----------------|
| **Transit Time** | Days to deliver | -20.0 to 0.0 | Faster delivery = higher score |
| **Processing Time** | Hours to process order | -10.0 to 0.0 | Faster processing = higher score |
| **Inventory Availability** | Stock level ratio | 0.0 to 100.0 | More inventory = higher score |
| **Distance** | Geographic proximity | -1.0 to 0.0 | Closer locations = higher score |
| **Express Priority** | Same-day/next-day bonus | 0.0 to 50.0 | Express-capable locations get bonus |
| **Split Penalty** | Multi-location penalty | 0.0 to 50.0 | Penalty for splitting orders |

### Pre-configured Scoring Profiles

#### 1. **Default Scoring** (`DEFAULT_SCORING`)
Standard configuration for general order fulfillment:
```json
{
  "transitTimeWeight": -10.0,
  "processingTimeWeight": -5.0,
  "inventoryWeight": 50.0,
  "expressWeight": 20.0,
  "splitPenaltyBase": 15.0,
  "distanceWeight": -0.5
}
```

#### 2. **Electronics Premium** (`ELECTRONICS_PREMIUM_SCORING`)
Enhanced configuration for high-value electronics:
```json
{
  "transitTimeWeight": -15.0,
  "processingTimeWeight": -8.0,
  "inventoryWeight": 60.0,
  "expressWeight": 30.0,
  "splitPenaltyBase": 20.0,
  "distanceWeight": -0.8,
  "highValueThreshold": 1000.0,
  "highValuePenalty": 30.0
}
```

#### 3. **Express Delivery** (`EXPRESS_DELIVERY_SCORING`)
Optimized for same-day and next-day delivery:
```json
{
  "transitTimeWeight": -20.0,
  "processingTimeWeight": -10.0,
  "inventoryWeight": 40.0,
  "expressWeight": 50.0,
  "splitPenaltyBase": 30.0,
  "distanceWeight": -1.0,
  "sameDayPenalty": 40.0,
  "nextDayPenalty": 30.0
}
```

#### 4. **Hazmat Handling** (`HAZMAT_SCORING`)
Specialized configuration for hazardous materials:
```json
{
  "transitTimeWeight": -12.0,
  "processingTimeWeight": -7.0,
  "inventoryWeight": 45.0,
  "expressWeight": 25.0,
  "splitPenaltyBase": 25.0,
  "distanceWeight": -0.6,
  "hazmatAdjustment": -0.30
}
```

## 🏭 Business Use Cases

### Use Case 1: Peak Season Optimization

**Challenge**: During Black Friday/Cyber Monday, some locations become overloaded while others have capacity.

**Solution**: Create a peak season scoring configuration:
```sql
INSERT INTO scoring_configuration (
    id, name, description, is_active, category,
    transit_time_weight, processing_time_weight, inventory_weight, express_weight,
    split_penalty_base, distance_weight, peak_season_adjustment
) VALUES (
    'PEAK_SEASON_2024', 'Peak Season Black Friday 2024', 
    'Adjusted scoring for peak season capacity management', 
    true, 'SEASONAL',
    -8.0, -3.0, 70.0, 15.0,
    10.0, -0.3, -0.20
);
```

**Business Impact**:
- **Reduced inventory weight** (-8.0 vs -10.0) prioritizes speed over stock levels
- **Increased inventory weight** (70.0 vs 50.0) ensures availability during high demand
- **Lower split penalty** (10.0 vs 15.0) allows more flexible fulfillment
- **Peak season adjustment** (-0.20) accounts for seasonal delays

### Use Case 2: Electronics Security Handling

**Challenge**: Electronics items need secure handling and trusted locations.

**Solution**: Use Electronics Premium scoring for electronics categories:
```json
{
  "sku": "LAPTOP_PREMIUM_001",
  "quantity": 1,
  "scoringConfigurationId": "ELECTRONICS_PREMIUM_SCORING",
  "productCategory": "ELECTRONICS_COMPUTER",
  "specialHandling": "HIGH_VALUE"
}
```

**Business Impact**:
- **Higher security penalties** for untrusted locations
- **Stricter distance requirements** for high-value items
- **Enhanced processing time penalties** for locations with poor security records

### Use Case 3: Geographic Expansion

**Challenge**: New market entry requires testing different location strategies.

**Solution**: Create region-specific scoring:
```sql
INSERT INTO scoring_configuration (
    id, name, description, is_active, category,
    transit_time_weight, processing_time_weight, inventory_weight, 
    distance_weight, distance_threshold
) VALUES (
    'WEST_COAST_EXPANSION', 'West Coast Market Entry', 
    'Testing scoring for new West Coast locations', 
    true, 'GEOGRAPHIC',
    -12.0, -6.0, 45.0, 
    -0.8, 75.0
);
```

**Business Impact**:
- **Tighter distance thresholds** (75km vs 100km) for new market testing
- **Balanced approach** between speed and reliability
- **A/B testing capability** against default scoring

## 📊 Location Filtering System

### Filter Categories

#### 1. **Delivery Type Filters**
- **Same Day Delivery** (`SDD_FILTER_RULE`): Locations capable of same-day delivery
- **Standard Delivery** (`STANDARD_DELIVERY_RULE`): General delivery locations
- **Express Delivery** (`EXPRESS_FILTER_RULE`): Next-day delivery capable locations

#### 2. **Product Category Filters**
- **Electronics Security** (`ELECTRONICS_SECURE_RULE`): Secure handling facilities
- **Frozen Food** (`FROZEN_FOOD_RULE`): Cold chain storage locations
- **Hazmat** (`HAZMAT_FILTER_RULE`): Hazardous material certified locations

#### 3. **Operational Filters**
- **Peak Season Capacity** (`PEAK_SEASON_RULE`): High-capacity locations during peak times
- **High Value Items** (`HIGH_VALUE_RULE`): Secure locations for expensive items

### Creating Custom Filters

```sql
INSERT INTO location_filter (
    id, name, description, filter_script, is_active, 
    category, execution_priority, cache_ttl_minutes
) VALUES (
    'CUSTOM_SAME_DAY_PRIORITY', 
    'Custom Same Day Priority Filter', 
    'Priority locations for same-day delivery in metro areas',
    'location.transitTime <= 1 && calculateDistance(location.latitude, location.longitude, order.latitude, order.longitude) <= 25',
    true, 'DELIVERY_TYPE', 1, 30
);
```

## 🚀 API Integration Examples

### Example 1: Fashion Retailer - Seasonal Adjustment

```bash
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "FASHION_SUMMER_001",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "orderItems": [
      {
        "sku": "SUMMER_DRESS_001",
        "quantity": 2,
        "deliveryType": "STANDARD",
        "locationFilterId": "STANDARD_DELIVERY_RULE",
        "scoringConfigurationId": "SEASONAL_SUMMER_2024",
        ,
        "productCategory": "FASHION_APPAREL"
      }
    ],
    "customerId": "FASHION_CUST_001",
    "customerTier": "PREMIUM",
    "isPeakSeason": true,
    "orderType": "WEB"
  }'
```

### Example 2: Electronics Store - High-Value Order

```bash
curl -X POST http://localhost:8081/api/sourcing/source-simplified \
  -H "Content-Type: application/json" \
  -d '{
    "tempOrderId": "ELECTRONICS_PREMIUM_001",
    "latitude": 37.7749,
    "longitude": -122.4194,
    "orderItems": [
      {
        "sku": "IPHONE_15_PRO",
        "quantity": 1,
        "deliveryType": "NEXT_DAY",
        "locationFilterId": "ELECTRONICS_SECURE_RULE",
        "scoringConfigurationId": "ELECTRONICS_PREMIUM_SCORING",
        ,
        "productCategory": "ELECTRONICS_MOBILE",
        "specialHandling": "HIGH_VALUE",
        "requiresSignature": true
      }
    ],
    "customerId": "ELECTRONICS_CUST_001",
    "customerTier": "ENTERPRISE",
    "orderType": "B2B"
  }'
```

### Example 3: Grocery Store - Cold Chain Items

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
        "temperatureRange": "FROZEN"
      }
    ],
    "customerId": "GROCERY_CUST_001",
    "customerTier": "STANDARD",
    "orderType": "WEB"
  }'
```

## 📈 Performance Monitoring

### Key Metrics to Track

1. **Fulfillment Success Rate**: Percentage of orders successfully fulfilled
2. **Average Score**: Overall scoring performance across orders
3. **Split Shipment Rate**: Percentage of orders requiring multiple locations
4. **Delivery Time Performance**: Actual vs. promised delivery times
5. **Location Utilization**: Distribution of orders across locations

### Scoring Analysis Query

```sql
SELECT 
    sc.name as scoring_config,
    COUNT(*) as order_count,
    AVG(fp.overall_score) as avg_score,
    AVG(fp.total_fulfilled::float / fp.requested_quantity) as fulfillment_rate,
    COUNT(CASE WHEN fp.is_multi_location_fulfillment THEN 1 END) as split_orders
FROM fulfillment_plans fp
JOIN scoring_configuration sc ON fp.scoring_config_id = sc.id
WHERE fp.created_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY sc.name
ORDER BY avg_score DESC;
```

## 🔄 Configuration Management

### Best Practices

1. **Version Control**: Always version your scoring configurations
2. **A/B Testing**: Test new configurations on a subset of orders
3. **Gradual Rollout**: Implement changes gradually to monitor impact
4. **Rollback Strategy**: Keep previous configurations active for quick rollback
5. **Documentation**: Document business rationale for each configuration

### Configuration Deployment Process

```sql
-- Step 1: Create new configuration (inactive)
INSERT INTO scoring_configuration (
    id, name, description, is_active, version, category,
    transit_time_weight, processing_time_weight, inventory_weight
) VALUES (
    'NEW_CONFIG_V2', 'New Configuration Version 2', 
    'Updated scoring for Q2 2024 optimization', 
    false, '2.0', 'SEASONAL',
    -12.0, -6.0, 55.0
);

-- Step 2: Test with specific order types
-- (Use API calls with specific scoringConfigurationId)

-- Step 3: Activate new configuration
UPDATE scoring_configuration 
SET is_active = true 
WHERE id = 'NEW_CONFIG_V2';

-- Step 4: Deactivate old configuration
UPDATE scoring_configuration 
SET is_active = false 
WHERE id = 'OLD_CONFIG_V1';
```

## 📋 Troubleshooting Guide

### Common Issues and Solutions

#### Issue 1: Low Fulfillment Rates
**Symptoms**: Many orders showing partial fulfillment
**Diagnosis**: Check inventory weights and thresholds
**Solution**: Increase inventory weight or adjust minimum thresholds

#### Issue 2: High Split Shipment Rate
**Symptoms**: Too many orders split across multiple locations
**Solution**: Increase split penalty or adjust inventory preferences

#### Issue 3: Poor Delivery Performance
**Symptoms**: Actual delivery times exceed promises
**Solution**: Adjust transit time and processing time weights

#### Issue 4: Unbalanced Location Utilization
**Symptoms**: Some locations overloaded, others underutilized
**Solution**: Adjust distance weights and location capacity factors

### Diagnostic Queries

```sql
-- Check scoring configuration usage
SELECT 
    sc.id,
    sc.name,
    COUNT(DISTINCT o.temp_order_id) as order_count,
    AVG(fp.overall_score) as avg_score
FROM scoring_configuration sc
LEFT JOIN fulfillment_plans fp ON fp.scoring_config_id = sc.id
LEFT JOIN orders o ON o.id = fp.order_id
WHERE sc.is_active = true
GROUP BY sc.id, sc.name
ORDER BY order_count DESC;

-- Analyze fulfillment patterns
SELECT 
    location_id,
    COUNT(*) as fulfillment_count,
    AVG(location_score) as avg_location_score,
    AVG(allocated_quantity) as avg_quantity
FROM location_allocations
WHERE created_date >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY location_id
ORDER BY fulfillment_count DESC;
```

This configuration system provides the flexibility to adapt your order sourcing strategy to changing business needs while maintaining optimal performance and customer satisfaction.