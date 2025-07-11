package com.ordersourcing.engine.service.impl;

import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.model.Location;
import com.ordersourcing.engine.model.ScoringConfiguration;
import com.ordersourcing.engine.repository.ScoringConfigurationRepository;
import com.ordersourcing.engine.service.ScoringConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ScoringConfigurationServiceImpl implements ScoringConfigurationService {
    
    @Autowired
    private ScoringConfigurationRepository scoringConfigurationRepository;
    
    private static final String DEFAULT_SCORING_CONFIG_ID = "DEFAULT_SCORING";
    
    /**
     * Gets the scoring configuration for a specific ID
     */
    private Optional<ScoringConfiguration> getScoringConfiguration(String configurationId) {
        log.debug("Getting scoring configuration for ID: {}", configurationId);
        return scoringConfigurationRepository.findByIdAndIsActiveTrue(configurationId);
    }
    
    /**
     * Gets the default scoring configuration when no specific configuration is provided
     */
    private ScoringConfiguration getDefaultScoringConfiguration() {
        log.debug("Getting default scoring configuration");
        return scoringConfigurationRepository.findByIdAndIsActiveTrue(DEFAULT_SCORING_CONFIG_ID)
                .orElseGet(this::createDefaultScoringConfiguration);
    }
    
    /**
     * Gets scoring configuration for an order item, falling back to default if not specified
     */
    @Override
    @Cacheable(value = "scoringConfigForItem", key = "#orderItem.scoringConfigurationId ?: 'DEFAULT'")
    public ScoringConfiguration getScoringConfigurationForItem(OrderItemDTO orderItem) {
        if (orderItem.getScoringConfigurationId() != null) {
            return getScoringConfiguration(orderItem.getScoringConfigurationId())
                    .orElse(getDefaultScoringConfiguration());
        }
        return getDefaultScoringConfiguration();
    }
    
    /**
     * Calculates location score using the specified scoring configuration
     */
    @Override
    public double calculateLocationScore(Location location, ScoringConfiguration config, 
                                       OrderItemDTO orderItem, Map<String, Object> context) {
        double score = 0;
        
        // Apply transit time weight (transitTime is primitive int, so no null check needed)
        score += location.getTransitTime() * config.getTransitTimeWeight();
        
        // Apply processing time weight (from context, as it comes from Inventory)
        if (context.containsKey("processingTime")) {
            int processingTime = (Integer) context.get("processingTime");
            score += processingTime * config.getProcessingTimeWeight();
        }
        
        // Apply inventory weight
        if (context.containsKey("inventoryRatio")) {
            double inventoryRatio = (Double) context.get("inventoryRatio");
            score += inventoryRatio * config.getInventoryWeight();
        }
        
        // Apply express weight for locations with short transit times
        if (orderItem.getIsExpressPriority() != null && orderItem.getIsExpressPriority() && 
            location.getTransitTime() <= 1) {
            score += config.getExpressWeight();
        }
        
        // Apply distance weight if available
        if (context.containsKey("distance")) {
            double distance = (Double) context.get("distance");
            if (distance <= config.getDistanceThreshold()) {
                score += distance * config.getDistanceWeight();
            }
        }
        
        log.debug("Calculated location score: {} for location: {} using config: {}", 
                  score, location.getId(), config.getId());
        
        return score;
    }
    
    /**
     * Calculates split penalty using the specified scoring configuration
     */
    @Override
    public double calculateSplitPenalty(int locationCount, double totalValue, 
                                      ScoringConfiguration config, OrderItemDTO orderItem) {
        if (locationCount <= 1) {
            return 0;
        }
        
        double penalty = config.getSplitPenaltyBase();
        
        // Apply exponential penalty
        penalty += Math.pow(locationCount - 1, config.getSplitPenaltyExponent()) * 
                   config.getSplitPenaltyMultiplier();
        
        // Apply high-value penalty
        if (totalValue > config.getHighValueThreshold()) {
            penalty += config.getHighValuePenalty();
        }
        
        // Apply urgency penalty
        if ("SAME_DAY".equals(orderItem.getDeliveryType())) {
            penalty += config.getSameDayPenalty();
        } else if ("NEXT_DAY".equals(orderItem.getDeliveryType())) {
            penalty += config.getNextDayPenalty();
        }
        
        log.debug("Calculated split penalty: {} for {} locations, value: {}, delivery type: {}", 
                  penalty, locationCount, totalValue, orderItem.getDeliveryType());
        
        return penalty;
    }
    
    /**
     * Gets scoring weights as a Map for script context
     */
    @Override
    public Map<String, Object> getScoringWeightsAsMap(ScoringConfiguration config) {
        Map<String, Object> weights = new HashMap<>();
        weights.put("transitTimeWeight", config.getTransitTimeWeight());
        weights.put("processingTimeWeight", config.getProcessingTimeWeight());
        weights.put("inventoryWeight", config.getInventoryWeight());
        weights.put("expressWeight", config.getExpressWeight());
        weights.put("splitPenaltyBase", config.getSplitPenaltyBase());
        weights.put("splitPenaltyExponent", config.getSplitPenaltyExponent());
        weights.put("splitPenaltyMultiplier", config.getSplitPenaltyMultiplier());
        weights.put("highValueThreshold", config.getHighValueThreshold());
        weights.put("highValuePenalty", config.getHighValuePenalty());
        weights.put("sameDayPenalty", config.getSameDayPenalty());
        weights.put("nextDayPenalty", config.getNextDayPenalty());
        weights.put("distanceWeight", config.getDistanceWeight());
        weights.put("distanceThreshold", config.getDistanceThreshold());
        weights.put("baseConfidence", config.getBaseConfidence());
        weights.put("peakSeasonAdjustment", config.getPeakSeasonAdjustment());
        weights.put("weatherAdjustment", config.getWeatherAdjustment());
        weights.put("hazmatAdjustment", config.getHazmatAdjustment());
        
        return weights;
    }
    
    /**
     * Creates a default scoring configuration with hard-coded values
     */
    private ScoringConfiguration createDefaultScoringConfiguration() {
        log.warn("Creating default scoring configuration - consider adding {} to database", 
                 DEFAULT_SCORING_CONFIG_ID);
        
        ScoringConfiguration config = new ScoringConfiguration();
        config.setId(DEFAULT_SCORING_CONFIG_ID);
        config.setName("Default Scoring Configuration");
        config.setDescription("Fallback scoring configuration with default weights");
        config.setIsActive(true);
        config.setCategory("DEFAULT");
        config.setExecutionPriority(999);
        
        // Set default weights (matching existing hard-coded values)
        config.setTransitTimeWeight(-10.0);
        config.setProcessingTimeWeight(-5.0);
        config.setInventoryWeight(50.0);
        config.setExpressWeight(20.0);
        config.setSplitPenaltyBase(15.0);
        config.setSplitPenaltyExponent(1.5);
        config.setSplitPenaltyMultiplier(10.0);
        config.setHighValueThreshold(500.0);
        config.setHighValuePenalty(20.0);
        config.setSameDayPenalty(25.0);
        config.setNextDayPenalty(15.0);
        config.setDistanceWeight(-0.5);
        config.setDistanceThreshold(100.0);
        config.setBaseConfidence(0.8);
        config.setPeakSeasonAdjustment(-0.1);
        config.setWeatherAdjustment(-0.05);
        config.setHazmatAdjustment(-0.15);
        
        return config;
    }
}