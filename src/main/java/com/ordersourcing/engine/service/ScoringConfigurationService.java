package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.model.ScoringConfiguration;
import com.ordersourcing.engine.model.Location;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ScoringConfigurationService {
    
    /**
     * Gets the scoring configuration for a specific ID
     */
    Optional<ScoringConfiguration> getScoringConfiguration(String configurationId);
    
    /**
     * Gets the default scoring configuration when no specific configuration is provided
     */
    ScoringConfiguration getDefaultScoringConfiguration();
    
    /**
     * Gets scoring configuration for an order item, falling back to default if not specified
     */
    ScoringConfiguration getScoringConfigurationForItem(OrderItemDTO orderItem);
    
    /**
     * Gets all active scoring configurations by category
     */
    List<ScoringConfiguration> getScoringConfigurationsByCategory(String category);
    
    /**
     * Gets all active scoring configurations ordered by priority
     */
    List<ScoringConfiguration> getAllActiveScoringConfigurations();
    
    /**
     * Batch loads scoring configurations for multiple items
     */
    Map<String, ScoringConfiguration> batchLoadScoringConfigurations(List<OrderItemDTO> orderItems);
    
    /**
     * Validates if a scoring configuration exists and is active
     */
    boolean isValidScoringConfiguration(String configurationId);
    
    /**
     * Calculates location score using the specified scoring configuration
     */
    double calculateLocationScore(Location location, ScoringConfiguration config, 
                                OrderItemDTO orderItem, Map<String, Object> context);
    
    /**
     * Calculates split penalty using the specified scoring configuration
     */
    double calculateSplitPenalty(int locationCount, double totalValue, 
                               ScoringConfiguration config, OrderItemDTO orderItem);
    
    /**
     * Refreshes the scoring configuration cache
     */
    void refreshCache();
    
    /**
     * Gets scoring weights as a Map for script context
     */
    Map<String, Object> getScoringWeightsAsMap(ScoringConfiguration config);
}