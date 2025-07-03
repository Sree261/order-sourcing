package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.OrderDTO;
import com.ordersourcing.engine.model.Location;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface LocationFilterExecutionService {
    
    /**
     * Execute location filter with intelligent caching
     */
    List<Location> executeLocationFilter(String filterId, OrderDTO orderContext);
    
    /**
     * Batch execute multiple filters in parallel
     */
    CompletableFuture<Map<String, List<Location>>> batchExecuteFilters(
            Set<String> filterIds, OrderDTO orderContext);
    
    /**
     * Pre-compute filter results for active filters
     */
    void refreshPrecomputedResults();
    
    /**
     * Invalidate cache for a specific filter
     */
    void invalidateFilterCache(String filterId);
    
    /**
     * Get filter performance metrics
     */
    Map<String, Object> getFilterMetrics();
}