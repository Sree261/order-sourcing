# Configurable Scoring System Implementation Plan

## Overview
This plan outlines the implementation of a configurable scoring system that allows businesses to adjust weightages of scoring parameters dynamically without code changes.

## Current State Analysis
The existing system has hardcoded scoring logic in `BatchSourcingServiceImpl.calculateLocationScore()` with fixed weights:
- Transit time scoring: `-10 * transitTime`
- Processing time scoring: `-5 * processingTime`
- Inventory availability: `+50 * inventoryRatio`
- Express priority bonus: `+20`

## Proposed Architecture

### 1. Data Model Design

#### ScoringConfiguration Entity
```
Table: scoring_configuration
- id (Primary Key)
- name (Unique identifier for the configuration)
- description (Business description)
- delivery_type (SAME_DAY, NEXT_DAY, STANDARD, ALL)
- customer_tier (PREMIUM, STANDARD, BASIC, ALL)
- order_type (WEB, MOBILE, PHONE, B2B, ALL)
- sales_channel (ECOMMERCE, RETAIL, B2B, ALL)
- is_active (Boolean)
- is_default (Boolean)
- priority (Integer - for conflict resolution)
- created_by, created_at, modified_by, modified_at
```

#### Scoring Weight Fields
```
Core Weights:
- transit_time_weight (Double)
- processing_time_weight (Double)
- inventory_weight (Double)
- distance_weight (Double)
- capacity_weight (Double)

Penalty/Bonus Weights:
- split_penalty_weight (Double)
- express_priority_bonus (Double)
- high_value_item_bonus (Double)
- same_day_delivery_bonus (Double)
- peak_season_adjustment (Double)
- customer_tier_bonus (Double)
- location_type_preference (Double)
```

### 2. Service Layer Architecture

#### ScoringConfigurationService
```java
public interface ScoringConfigurationService {
    // Configuration Management
    ScoringConfiguration createConfiguration(CreateScoringConfigRequest request);
    ScoringConfiguration updateConfiguration(Long id, UpdateScoringConfigRequest request);
    void deleteConfiguration(Long id);
    List<ScoringConfiguration> getAllConfigurations();
    ScoringConfiguration getConfigurationById(Long id);
    
    // Configuration Resolution
    ScoringConfiguration resolveConfiguration(OrderDTO order, OrderItemDTO item);
    ScoringWeights getScoringWeights(OrderDTO order, OrderItemDTO item);
    
    // Configuration Validation
    void validateConfiguration(ScoringConfiguration config);
    List<ValidationError> validateWeights(ScoringWeights weights);
    
    // Configuration Management
    void activateConfiguration(Long id);
    void deactivateConfiguration(Long id);
    void setAsDefault(Long id);
}
```

#### ScoringWeights Value Object
```java
@Data
@Builder
public class ScoringWeights {
    private Double transitTimeWeight;
    private Double processingTimeWeight;
    private Double inventoryWeight;
    private Double distanceWeight;
    private Double capacityWeight;
    private Double splitPenaltyWeight;
    private Double expressPriorityBonus;
    private Double highValueItemBonus;
    private Double sameDayDeliveryBonus;
    private Double peakSeasonAdjustment;
    private Double customerTierBonus;
    private Double locationTypePreference;
}
```

### 3. Configuration Resolution Strategy

#### Priority-Based Resolution
1. **Exact Match**: delivery_type + customer_tier + order_type + sales_channel
2. **Partial Match**: Progressively relax constraints (replace with "ALL")
3. **Default Match**: Use configuration marked as default
4. **Fallback**: Use hardcoded system defaults

#### Resolution Algorithm
```
1. Find all active configurations
2. Filter by exact criteria match
3. If multiple matches, select by priority (lowest number wins)
4. If no exact match, try partial matches in order:
   - ALL delivery_type + specific customer_tier + specific order_type
   - Specific delivery_type + ALL customer_tier + specific order_type
   - Specific delivery_type + specific customer_tier + ALL order_type
   - ALL delivery_type + ALL customer_tier + specific order_type
   - etc.
5. If no match found, use default configuration
6. If no default, use system hardcoded defaults
```

### 4. Caching Strategy

#### Multi-Level Caching
```java
@Service
public class CachedScoringConfigurationService {
    // L1 Cache: In-memory configuration cache
    @Cacheable("scoring-configurations")
    public List<ScoringConfiguration> getAllActiveConfigurations();
    
    // L2 Cache: Resolved weights cache
    @Cacheable("scoring-weights")
    public ScoringWeights getScoringWeights(String cacheKey);
    
    // Cache invalidation on configuration changes
    @CacheEvict(value = {"scoring-configurations", "scoring-weights"}, allEntries = true)
    public void invalidateCache();
}
```

#### Cache Key Strategy
```
Cache Key Format: {deliveryType}:{customerTier}:{orderType}:{salesChannel}
Examples:
- "SAME_DAY:PREMIUM:WEB:ECOMMERCE"
- "STANDARD:BASIC:PHONE:RETAIL"
- "ALL:ALL:ALL:ALL" (default)
```

### 5. Enhanced Scoring Algorithm

#### Refactored calculateLocationScore Method
```java
private double calculateLocationScore(Location location, Inventory inventory, 
                                    OrderItemDTO orderItem, OrderDTO order, 
                                    ScoringWeights weights) {
    double score = 0;
    
    // Core factors
    score += calculateTransitTimeScore(location.getTransitTime(), weights.getTransitTimeWeight());
    score += calculateProcessingTimeScore(inventory.getProcessingTime(), weights.getProcessingTimeWeight());
    score += calculateInventoryScore(inventory.getQuantity(), orderItem.getQuantity(), weights.getInventoryWeight());
    score += calculateDistanceScore(location, order, weights.getDistanceWeight());
    score += calculateCapacityScore(location, weights.getCapacityWeight());
    
    // Bonus/Penalty factors
    score += calculateExpressPriorityBonus(orderItem, weights.getExpressPriorityBonus());
    score += calculateHighValueItemBonus(orderItem, weights.getHighValueItemBonus());
    score += calculateSameDayDeliveryBonus(orderItem, weights.getSameDayDeliveryBonus());
    score += calculatePeakSeasonAdjustment(order, weights.getPeakSeasonAdjustment());
    score += calculateCustomerTierBonus(order, weights.getCustomerTierBonus());
    score += calculateLocationTypePreference(location, weights.getLocationTypePreference());
    
    return score;
}
```

### 6. REST API Design

#### Configuration Management Endpoints
```
GET    /api/scoring-configurations              - List all configurations
GET    /api/scoring-configurations/{id}         - Get specific configuration
POST   /api/scoring-configurations              - Create new configuration
PUT    /api/scoring-configurations/{id}         - Update configuration
DELETE /api/scoring-configurations/{id}         - Delete configuration
POST   /api/scoring-configurations/{id}/activate - Activate configuration
POST   /api/scoring-configurations/{id}/deactivate - Deactivate configuration
POST   /api/scoring-configurations/{id}/set-default - Set as default
```

#### Testing and Simulation Endpoints
```
POST   /api/scoring-configurations/test         - Test configuration against sample orders
POST   /api/scoring-configurations/simulate     - Simulate scoring with different weights
GET    /api/scoring-configurations/resolve      - Resolve configuration for given criteria
```

### 7. Database Schema

#### Migration Script
```sql
CREATE TABLE scoring_configuration (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    delivery_type VARCHAR(50) NOT NULL,
    customer_tier VARCHAR(50) NOT NULL,
    order_type VARCHAR(50) NOT NULL,
    sales_channel VARCHAR(50) NOT NULL DEFAULT 'ALL',
    
    -- Core weights
    transit_time_weight DECIMAL(5,2) NOT NULL DEFAULT 10.0,
    processing_time_weight DECIMAL(5,2) NOT NULL DEFAULT 5.0,
    inventory_weight DECIMAL(5,2) NOT NULL DEFAULT 50.0,
    distance_weight DECIMAL(5,2) NOT NULL DEFAULT 15.0,
    capacity_weight DECIMAL(5,2) NOT NULL DEFAULT 10.0,
    
    -- Bonus/Penalty weights
    split_penalty_weight DECIMAL(5,2) NOT NULL DEFAULT 15.0,
    express_priority_bonus DECIMAL(5,2) NOT NULL DEFAULT 20.0,
    high_value_item_bonus DECIMAL(5,2) NOT NULL DEFAULT 25.0,
    same_day_delivery_bonus DECIMAL(5,2) NOT NULL DEFAULT 30.0,
    peak_season_adjustment DECIMAL(5,2) NOT NULL DEFAULT 10.0,
    customer_tier_bonus DECIMAL(5,2) NOT NULL DEFAULT 15.0,
    location_type_preference DECIMAL(5,2) NOT NULL DEFAULT 5.0,
    
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_default BOOLEAN NOT NULL DEFAULT false,
    priority INTEGER NOT NULL DEFAULT 100,
    
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(255),
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_scoring_config_criteria ON scoring_configuration(delivery_type, customer_tier, order_type, sales_channel);
CREATE INDEX idx_scoring_config_active ON scoring_configuration(is_active);
CREATE INDEX idx_scoring_config_default ON scoring_configuration(is_default);
CREATE INDEX idx_scoring_config_priority ON scoring_configuration(priority);
```

#### Sample Data
```sql
-- Default configuration for all scenarios
INSERT INTO scoring_configuration (
    name, description, delivery_type, customer_tier, order_type, sales_channel,
    transit_time_weight, processing_time_weight, inventory_weight, distance_weight, capacity_weight,
    split_penalty_weight, express_priority_bonus, high_value_item_bonus, same_day_delivery_bonus,
    peak_season_adjustment, customer_tier_bonus, location_type_preference,
    is_active, is_default, priority, created_by
) VALUES 
('Default Configuration', 'Default scoring weights for all scenarios', 'ALL', 'ALL', 'ALL', 'ALL',
 10.0, 5.0, 50.0, 15.0, 10.0, 15.0, 20.0, 25.0, 30.0, 10.0, 15.0, 5.0,
 true, true, 999, 'system');

-- Premium customer same-day delivery configuration
INSERT INTO scoring_configuration (
    name, description, delivery_type, customer_tier, order_type, sales_channel,
    transit_time_weight, processing_time_weight, inventory_weight, distance_weight, capacity_weight,
    split_penalty_weight, express_priority_bonus, high_value_item_bonus, same_day_delivery_bonus,
    peak_season_adjustment, customer_tier_bonus, location_type_preference,
    is_active, is_default, priority, created_by
) VALUES 
('Premium Same Day', 'Optimized for premium customers with same-day delivery', 'SAME_DAY', 'PREMIUM', 'ALL', 'ALL',
 50.0, 30.0, 40.0, 25.0, 20.0, 5.0, 50.0, 40.0, 60.0, 5.0, 30.0, 10.0,
 true, false, 1, 'system');
```

### 8. Configuration Validation Rules

#### Business Rules
1. **Weight Constraints**: All weights must be >= 0 and <= 1000
2. **Uniqueness**: No duplicate configurations for same criteria combination
3. **Default Constraint**: Only one default configuration allowed
4. **Priority Constraint**: Priority must be between 1-999
5. **Sum Validation**: Core weights should sum to reasonable total (optional business rule)

#### Validation Implementation
```java
public class ScoringConfigurationValidator {
    public List<ValidationError> validate(ScoringConfiguration config) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Weight range validation
        validateWeightRange(config.getTransitTimeWeight(), "transitTimeWeight", errors);
        validateWeightRange(config.getProcessingTimeWeight(), "processingTimeWeight", errors);
        // ... other weights
        
        // Business rule validation
        validateUniqueness(config, errors);
        validateDefaultConstraint(config, errors);
        validatePriorityRange(config, errors);
        
        return errors;
    }
}
```

### 9. Implementation Phase Plan

#### Phase 1: Core Infrastructure (Week 1)
- [ ] Create ScoringConfiguration entity and repository
- [ ] Implement basic ScoringConfigurationService
- [ ] Create ScoringWeights value object
- [ ] Database migration and sample data

#### Phase 2: Configuration Resolution (Week 2)
- [ ] Implement configuration resolution algorithm
- [ ] Add caching layer
- [ ] Create configuration validation
- [ ] Unit tests for resolution logic

#### Phase 3: Integration (Week 3)
- [ ] Modify BatchSourcingServiceImpl to use configurable weights
- [ ] Refactor calculateLocationScore method
- [ ] Add enhanced scoring factors
- [ ] Integration tests

#### Phase 4: API and Management (Week 4)
- [ ] Create REST API endpoints
- [ ] Add configuration management UI (if needed)
- [ ] Performance testing and optimization
- [ ] Documentation and deployment

### 10. Testing Strategy

#### Unit Tests
- Configuration resolution algorithm
- Scoring weight calculation
- Validation logic
- Cache behavior

#### Integration Tests
- End-to-end order sourcing with different configurations
- Configuration management API
- Performance benchmarks
- Edge case scenarios

#### Performance Tests
- Cache hit rate optimization
- Configuration resolution performance
- Impact on overall sourcing time
- Memory usage with multiple configurations

### 11. Migration Strategy

#### Backward Compatibility
- Maintain existing hardcoded scoring as fallback
- Gradual rollout with feature flags
- A/B testing capability
- Rollback procedures

#### Data Migration
- Create default configuration matching current hardcoded values
- Migrate existing business rules to configurations
- Training for business users on new configuration system

### 12. Monitoring and Observability

#### Metrics to Track
- Configuration resolution performance
- Cache hit rates
- Scoring calculation time
- Configuration usage patterns
- Business impact of weight changes

#### Alerting
- Configuration resolution failures
- Performance degradation
- Cache invalidation events
- Unusual scoring patterns

## Success Criteria

1. **Performance**: Configuration resolution adds < 5ms to sourcing time
2. **Flexibility**: Business users can create/modify configurations without code changes
3. **Reliability**: System maintains 99.9% uptime during configuration changes
4. **Usability**: Configuration changes take effect within 5 minutes
5. **Maintainability**: Clear audit trail of all configuration changes

## Risk Mitigation

1. **Performance Risk**: Extensive caching and optimization
2. **Complexity Risk**: Phased implementation with thorough testing
3. **Business Risk**: Comprehensive validation and rollback procedures
4. **Operational Risk**: Monitoring and alerting for early issue detection