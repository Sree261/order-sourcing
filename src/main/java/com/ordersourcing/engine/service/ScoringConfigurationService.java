package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.model.ScoringConfiguration;
import com.ordersourcing.engine.model.Location;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ScoringConfigurationService {
    
    /**
     * Gets scoring configuration for an order item, falling back to default if not specified
     */
    ScoringConfiguration getScoringConfigurationForItem(OrderItemDTO orderItem);
    
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
     * Gets scoring weights as a Map for script context
     */
    Map<String, Object> getScoringWeightsAsMap(ScoringConfiguration config);
}