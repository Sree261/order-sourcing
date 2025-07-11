package com.ordersourcing.engine.service.impl;

import com.ordersourcing.engine.dto.*;
import com.ordersourcing.engine.model.*;
import com.ordersourcing.engine.service.BatchSourcingService;
import com.ordersourcing.engine.service.LocationFilterExecutionService;
import com.ordersourcing.engine.service.InventoryApiService;
import com.ordersourcing.engine.service.PromiseDateService;
import com.ordersourcing.engine.service.ScoringConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BatchSourcingServiceImpl implements BatchSourcingService {
    
    @Autowired
    private LocationFilterExecutionService locationFilterService;
    
    @Autowired
    private InventoryApiService inventoryApiService;
    
    @Autowired
    private PromiseDateService promiseDateService;
    
    @Autowired
    private ScoringConfigurationService scoringConfigurationService;
    
    // Configuration for batch vs sequential decision
    private static final int BATCH_THRESHOLD_ITEMS = 3;
    private static final int BATCH_THRESHOLD_TOTAL_QUANTITY = 10;
    
    
    /**
     * Main sourcing method that returns essential fulfillment information
     */
    public SourcingResponse sourceOrder(OrderDTO order) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Decide processing strategy
            SourcingStrategy strategy = decideSourcingStrategy(order);
            log.info("Using {} strategy for order: {} with {} items", 
                    strategy, order.getTempOrderId(), order.getOrderItems().size());
            
            List<SourcingResponse.FulfillmentPlan> fulfillmentPlans;
            if (strategy == SourcingStrategy.BATCH) {
                fulfillmentPlans = batchSourceOrder(order);
            } else {
                fulfillmentPlans = sequentialSourceOrder(order);
            }
            
            return SourcingResponse.builder()
                    .orderId(order.getTempOrderId())
                    .fulfillmentPlans(fulfillmentPlans)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            
        } catch (Exception e) {
            log.error("Error in order sourcing for order: {}", order.getTempOrderId(), e);
            return createErrorResponse(order, e, System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * Optimized batch processing for multiple items - simplified version
     */
    private List<SourcingResponse.FulfillmentPlan> batchSourceOrder(OrderDTO order) {
        log.debug("Starting batch sourcing for order: {}", order.getTempOrderId());
        
        // Step 1: Group OrderItems by LocationFilter ID to eliminate duplicate executions
        Map<String, List<OrderItemDTO>> filterGroups = order.getOrderItems().stream()
                .collect(Collectors.groupingBy(OrderItemDTO::getLocationFilterId));
        
        log.debug("Grouped {} items into {} filter groups", order.getOrderItems().size(), filterGroups.size());
        
        // Step 2: Parallel filter execution (one per unique filter)
        CompletableFuture<Map<String, List<Location>>> filterFuture = 
                locationFilterService.batchExecuteFilters(filterGroups.keySet(), order);
        
        // Step 3: Parallel inventory API call
        CompletableFuture<Map<String, List<Inventory>>> inventoryFuture = 
                inventoryApiService.batchFetchInventory(order.getOrderItems());
        
        // Wait for both parallel operations to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(filterFuture, inventoryFuture);
        
        try {
            allFutures.join(); // Wait for completion
            
            Map<String, List<Location>> filterResults = filterFuture.get();
            Map<String, List<Inventory>> inventoryResults = inventoryFuture.get();
            
            // Step 4: Parallel promise date calculation
            CompletableFuture<Map<String, PromiseDateBreakdown>> promiseDateFuture = 
                    promiseDateService.batchCalculatePromiseDates(order.getOrderItems(), 
                            filterResults, inventoryResults, order);
            
            Map<String, PromiseDateBreakdown> promiseDateResults = promiseDateFuture.get();
            
            // Step 5: Build simplified fulfillment plans
            return buildSimplifiedFulfillmentPlans(order.getOrderItems(), filterResults, 
                    inventoryResults, promiseDateResults, order);
                    
        } catch (Exception e) {
            log.error("Error in batch processing", e);
            throw new RuntimeException("Batch processing failed", e);
        }
    }
    
    /**
     * Sequential processing for simple orders - simplified version
     */
    private List<SourcingResponse.FulfillmentPlan> sequentialSourceOrder(OrderDTO order) {
        log.debug("Starting sequential sourcing for order: {}", order.getTempOrderId());
        
        List<SourcingResponse.FulfillmentPlan> fulfillmentPlans = new ArrayList<>();
        
        for (OrderItemDTO orderItem : order.getOrderItems()) {
            try {
                // Filter execution
                List<Location> locations = locationFilterService.executeLocationFilter(
                        orderItem.getLocationFilterId(), order);
                
                if (locations.isEmpty()) {
                    log.warn("No locations found for item: {} with filter: {}", 
                            orderItem.getSku(), orderItem.getLocationFilterId());
                    continue;
                }
                
                // Inventory fetch
                List<Inventory> inventories = inventoryApiService.fetchInventoryBySku(orderItem.getSku());
                
                if (inventories.isEmpty()) {
                    log.warn("No inventory found for SKU: {}", orderItem.getSku());
                    continue;
                }
                
                // Find optimal fulfillment strategy
                FulfillmentStrategy strategy = findOptimalFulfillmentStrategy(
                        locations, inventories, orderItem, order);
                
                if (strategy != null) {
                    // Promise date calculation (use primary location for timing)
                    LocationInventoryPair primaryPair = strategy.allocations.get(0);
                    PromiseDateBreakdown promiseDate = promiseDateService.calculateEnhancedPromiseDate(
                            orderItem, primaryPair.location, primaryPair.inventory, order);
                    
                    // Only add fulfillment plan if promise date calculation was successful
                    if (promiseDate != null) {
                        // Build simplified fulfillment plan
                        SourcingResponse.FulfillmentPlan plan = buildFulfillmentPlan(
                                orderItem, strategy, promiseDate, order);
                        fulfillmentPlans.add(plan);
                    } else {
                        log.info("Skipping fulfillment plan for item {} - delivery type {} not feasible", 
                                orderItem.getSku(), orderItem.getDeliveryType());
                    }
                }
                
            } catch (Exception e) {
                log.error("Error processing item: {}", orderItem.getSku(), e);
            }
        }
        
        return fulfillmentPlans;
    }
    
    /**
     * Smart decision engine for batch vs sequential processing
     */
    private SourcingStrategy decideSourcingStrategy(OrderDTO order) {
        int itemCount = order.getOrderItems().size();
        int totalQuantity = order.getTotalQuantity();
        boolean hasMultipleDeliveryTypes = order.hasMultipleDeliveryTypes();
        boolean hasTimeSensitiveItems = order.hasTimeSensitiveItems();
        
        // Force sequential for single simple items
        if (itemCount == 1 && !hasTimeSensitiveItems && totalQuantity <= 5) {
            return SourcingStrategy.SEQUENTIAL;
        }
        
        // Force batch for large or complex orders
        if (itemCount >= BATCH_THRESHOLD_ITEMS || 
            totalQuantity >= BATCH_THRESHOLD_TOTAL_QUANTITY ||
            hasMultipleDeliveryTypes ||
            order.isLargeOrder()) {
            return SourcingStrategy.BATCH;
        }
        
        // Default to sequential for simple orders
        return SourcingStrategy.SEQUENTIAL;
    }
    
    /**
     * Build simplified fulfillment plans from batch results
     */
    private List<SourcingResponse.FulfillmentPlan> buildSimplifiedFulfillmentPlans(
            List<OrderItemDTO> orderItems,
            Map<String, List<Location>> filterResults,
            Map<String, List<Inventory>> inventoryResults,
            Map<String, PromiseDateBreakdown> promiseDateResults,
            OrderDTO order) {
        
        List<SourcingResponse.FulfillmentPlan> plans = new ArrayList<>();
        
        for (OrderItemDTO orderItem : orderItems) {
            try {
                List<Location> locations = filterResults.get(orderItem.getLocationFilterId());
                List<Inventory> inventories = inventoryResults.get(orderItem.getSku());
                PromiseDateBreakdown promiseDate = promiseDateResults.get(orderItem.getSku());
                
                if (locations != null && !locations.isEmpty() && 
                    inventories != null && !inventories.isEmpty()) {
                    
                    FulfillmentStrategy strategy = findOptimalFulfillmentStrategy(
                            locations, inventories, orderItem, order);
                    
                    if (strategy != null && promiseDate != null) {
                        SourcingResponse.FulfillmentPlan plan = buildFulfillmentPlan(
                                orderItem, strategy, promiseDate, order);
                        plans.add(plan);
                    }
                }
            } catch (Exception e) {
                log.error("Error building fulfillment plan for item: {}", orderItem.getSku(), e);
            }
        }
        
        return plans;
    }
    
    /**
     * Find optimal multi-location fulfillment strategy for an item
     */
    private FulfillmentStrategy findOptimalFulfillmentStrategy(
            List<Location> locations, List<Inventory> inventories, OrderItemDTO orderItem, OrderDTO order) {
        
        // Get all viable location-inventory pairs
        List<LocationInventoryPair> availablePairs = new ArrayList<>();
        for (Location location : locations) {
            for (Inventory inventory : inventories) {
                if (inventory.getLocationId().equals(location.getId()) && inventory.getQuantity() > 0) {
                    double score = calculateLocationScore(location, inventory, orderItem);
                    availablePairs.add(new LocationInventoryPair(location, inventory, score));
                }
            }
        }
        
        if (availablePairs.isEmpty()) {
            return null;
        }
        
        // Sort by score (best first)
        availablePairs.sort((a, b) -> Double.compare(b.score, a.score));
        
        // Strategy 1: Single location (if possible)
        FulfillmentStrategy singleLocationStrategy = evaluateSingleLocationStrategy(availablePairs, orderItem, order);
        
        // Strategy 2: Multi-location (greedy allocation)  
        FulfillmentStrategy multiLocationStrategy = evaluateMultiLocationStrategy(availablePairs, orderItem, order);
        
        // Choose the best strategy (single location preferred due to no split penalty)
        if (singleLocationStrategy != null && multiLocationStrategy != null) {
            return singleLocationStrategy.overallScore >= multiLocationStrategy.overallScore 
                   ? singleLocationStrategy : multiLocationStrategy;
        }
        
        return singleLocationStrategy != null ? singleLocationStrategy : multiLocationStrategy;
    }
    
    /**
     * Evaluate single location fulfillment strategy
     */
    private FulfillmentStrategy evaluateSingleLocationStrategy(
            List<LocationInventoryPair> pairs, OrderItemDTO orderItem, OrderDTO order) {
        
        LocationInventoryPair bestPair = pairs.get(0); // Already sorted by score
        
        int quantityToFulfill = Math.min(bestPair.inventory.getQuantity(), orderItem.getQuantity());
        boolean isPartialFulfillment = bestPair.inventory.getQuantity() < orderItem.getQuantity();
        
        // Check if full quantity is required (all or nothing)
        if (requiresFullQuantity(orderItem) && isPartialFulfillment) {
            log.debug("Full quantity required for item: {} in order: {}, but only partial available, skipping strategy", 
                     orderItem.getSku(), order.getTempOrderId());
            return null; // No strategy if full quantity required but not available
        }
        
        // Check if partial fulfillment is allowed for this item
        if (isPartialFulfillment && !isPartialFulfillmentAllowed(orderItem, order)) {
            log.debug("Partial fulfillment not allowed for item: {} in order: {}, skipping single location strategy", 
                     orderItem.getSku(), order.getTempOrderId());
            return null; // No strategy if partial not allowed
        }
        
        List<LocationInventoryPair> allocations = List.of(
            new LocationInventoryPair(bestPair.location, bestPair.inventory, bestPair.score, quantityToFulfill)
        );
        
        double overallScore = bestPair.score; // No split penalty for single location
        
        return new FulfillmentStrategy(allocations, quantityToFulfill, isPartialFulfillment, 
                                     false, overallScore, 0.0);
    }
    
    /**
     * Evaluate multi-location fulfillment strategy (greedy allocation)
     */
    private FulfillmentStrategy evaluateMultiLocationStrategy(
            List<LocationInventoryPair> pairs, OrderItemDTO orderItem, OrderDTO order) {
        
        List<LocationInventoryPair> allocations = new ArrayList<>();
        int remainingQuantity = orderItem.getQuantity();
        double totalWeightedScore = 0.0;
        int totalAllocated = 0;
        
        // Greedy allocation: fill from best locations first
        for (LocationInventoryPair pair : pairs) {
            if (remainingQuantity <= 0) break;
            
            int allocationQuantity = Math.min(pair.inventory.getQuantity(), remainingQuantity);
            allocations.add(new LocationInventoryPair(pair.location, pair.inventory, 
                                                    pair.score, allocationQuantity));
            
            totalWeightedScore += pair.score * allocationQuantity;
            totalAllocated += allocationQuantity;
            remainingQuantity -= allocationQuantity;
        }
        
        if (allocations.isEmpty()) {
            return null;
        }
        
        // Calculate average weighted score
        double baseScore = totalWeightedScore / totalAllocated;
        
        // Apply split penalty (configurable)
        double splitPenalty = calculateSplitPenalty(allocations.size(), orderItem);
        double overallScore = baseScore - splitPenalty;
        
        boolean isPartialFulfillment = totalAllocated < orderItem.getQuantity();
        boolean isMultiLocation = allocations.size() > 1;
        
        // Check if full quantity is required (all or nothing)
        if (requiresFullQuantity(orderItem) && isPartialFulfillment) {
            log.debug("Full quantity required for item: {} in order: {}, but only partial available across all locations", 
                     orderItem.getSku(), order.getTempOrderId());
            return null; // No strategy if full quantity required but not available
        }
        
        // Check fulfillment policies for this item
        if (isPartialFulfillment && !isPartialFulfillmentAllowed(orderItem, order)) {
            log.debug("Partial fulfillment not allowed for item: {} in order: {}, skipping multi-location strategy", 
                     orderItem.getSku(), order.getTempOrderId());
            return null;
        }
        
        if (isMultiLocation && prefersSingleLocation(orderItem, order)) {
            // Apply additional penalty for items that prefer single location
            overallScore -= 50.0; // Heavy penalty
            log.debug("Item {} prefers single location, applying additional penalty for order: {}", 
                     orderItem.getSku(), order.getTempOrderId());
        }
        
        return new FulfillmentStrategy(allocations, totalAllocated, isPartialFulfillment, 
                                     isMultiLocation, overallScore, splitPenalty);
    }
    
    /**
     * Calculate penalty for splitting shipments across multiple locations using configurable weights
     */
    private double calculateSplitPenalty(int locationCount, OrderItemDTO orderItem) {
        if (locationCount <= 1) return 0.0;
        
        // Get scoring configuration for this order item
        var scoringConfig = scoringConfigurationService.getScoringConfigurationForItem(orderItem);
        
        // Use configurable scoring service (without value-based penalties)
        double penalty = scoringConfigurationService.calculateSplitPenalty(
            locationCount, 0.0, scoringConfig, orderItem);
        
        log.debug("Calculated split penalty: {} for {} locations, item: {}, using config: {}", 
                  penalty, locationCount, orderItem.getSku(), scoringConfig.getId());
        
        return penalty;
    }
    
    /**
     * Check if partial fulfillment is allowed for this item
     */
    private boolean isPartialFulfillmentAllowed(OrderItemDTO orderItem, OrderDTO order) {
        // Item-level setting takes precedence over order-level setting
        if (orderItem.getAllowPartialFulfillment() != null) {
            return orderItem.getAllowPartialFulfillment();
        }
        
        // Fall back to order-level setting
        if (order.getAllowPartialShipments() != null) {
            return order.getAllowPartialShipments();
        }
        
        // Default to true if not specified at any level
        return true;
    }
    
    /**
     * Check if this item prefers single location fulfillment
     */
    private boolean prefersSingleLocation(OrderItemDTO orderItem, OrderDTO order) {
        // Item-level setting takes precedence over order-level setting
        if (orderItem.getPreferSingleLocation() != null) {
            return orderItem.getPreferSingleLocation();
        }
        
        // Fall back to order-level setting
        if (order.getPreferSingleLocation() != null) {
            return order.getPreferSingleLocation();
        }
        
        // Default to false if not specified at any level
        return false;
    }
    
    /**
     * Check if this item requires full quantity fulfillment (all or nothing)
     */
    private boolean requiresFullQuantity(OrderItemDTO orderItem) {
        return orderItem.getRequireFullQuantity() != null && orderItem.getRequireFullQuantity();
    }
    
    /**
     * Check if backorders are allowed for this item
     */
    private boolean isBackorderAllowed(OrderItemDTO orderItem, OrderDTO order) {
        // Item-level setting takes precedence over order-level setting
        if (orderItem.getAllowBackorder() != null) {
            return orderItem.getAllowBackorder();
        }
        
        // Fall back to order-level setting
        if (order.getAllowBackorders() != null) {
            return order.getAllowBackorders();
        }
        
        // Default to false if not specified at any level
        return false;
    }
    
    /**
     * Calculate location score based on multiple factors using configurable weights
     */
    private double calculateLocationScore(Location location, Inventory inventory, OrderItemDTO orderItem) {
        // Get scoring configuration for this order item
        var scoringConfig = scoringConfigurationService.getScoringConfigurationForItem(orderItem);
        
        // Create context for scoring calculation
        Map<String, Object> context = new HashMap<>();
        double inventoryRatio = Math.min(1.0, (double) inventory.getQuantity() / orderItem.getQuantity());
        context.put("inventoryRatio", inventoryRatio);
        context.put("processingTime", inventory.getProcessingTime());
        
        // Use configurable scoring service
        double score = scoringConfigurationService.calculateLocationScore(location, scoringConfig, orderItem, context);
        
        log.debug("Calculated location score: {} for location: {}, item: {}, using config: {}", 
                  score, location.getId(), orderItem.getSku(), scoringConfig.getId());
        
        return score;
    }
    
    /**
     * Build fulfillment plan
     */
    private SourcingResponse.FulfillmentPlan buildFulfillmentPlan(
            OrderItemDTO orderItem, FulfillmentStrategy strategy, 
            PromiseDateBreakdown promiseDate, OrderDTO order) {
        
        List<SourcingResponse.LocationAllocation> locationAllocations = new ArrayList<>();
        
        // Only include locations that are actually allocated in the optimal plan
        for (LocationInventoryPair pair : strategy.allocations) {
            if (pair.allocatedQuantity > 0) {
                SourcingResponse.DeliveryTiming deliveryTiming = 
                    SourcingResponse.DeliveryTiming.builder()
                        .estimatedShipDate(promiseDate.getCarrierPickupTime())
                        .estimatedDeliveryDate(promiseDate.getEstimatedDeliveryDate())
                        .transitTimeDays(pair.location.getTransitTime())
                        .processingTimeHours(pair.inventory.getProcessingTime() * 24)
                        .build();
                
                SourcingResponse.LocationAllocation allocation = 
                    SourcingResponse.LocationAllocation.builder()
                        .locationId(pair.location.getId())
                        .locationName(pair.location.getName())
                        .allocatedQuantity(pair.allocatedQuantity)
                        .locationScore(pair.score)
                        .deliveryTiming(deliveryTiming)
                        .build();
                
                locationAllocations.add(allocation);
            }
        }
        
        return SourcingResponse.FulfillmentPlan.builder()
                .sku(orderItem.getSku())
                .requestedQuantity(orderItem.getQuantity())
                .totalFulfilled(strategy.totalFulfilled)
                .isPartialFulfillment(strategy.isPartialFulfillment)
                .overallScore(strategy.overallScore)
                .locationAllocations(locationAllocations)
                .build();
    }
    
    
    private SourcingResponse createErrorResponse(OrderDTO order, Exception e, long processingTime) {
        return SourcingResponse.builder()
                .orderId(order.getTempOrderId())
                .fulfillmentPlans(Collections.emptyList())
                .processingTimeMs(processingTime)
                .build();
    }
    
    
    // Helper classes
    private static class LocationInventoryPair {
        final Location location;
        final Inventory inventory;
        final double score;
        final int allocatedQuantity;
        
        LocationInventoryPair(Location location, Inventory inventory) {
            this.location = location;
            this.inventory = inventory;
            this.score = 0.0;
            this.allocatedQuantity = 0;
        }
        
        LocationInventoryPair(Location location, Inventory inventory, double score) {
            this.location = location;
            this.inventory = inventory;
            this.score = score;
            this.allocatedQuantity = 0;
        }
        
        LocationInventoryPair(Location location, Inventory inventory, double score, int allocatedQuantity) {
            this.location = location;
            this.inventory = inventory;
            this.score = score;
            this.allocatedQuantity = allocatedQuantity;
        }
    }
    
    private static class FulfillmentStrategy {
        final List<LocationInventoryPair> allocations;
        final int totalFulfilled;
        final boolean isPartialFulfillment;
        final boolean isMultiLocation;
        final double overallScore;
        final double splitPenalty;
        
        FulfillmentStrategy(List<LocationInventoryPair> allocations, int totalFulfilled, 
                          boolean isPartialFulfillment, boolean isMultiLocation, 
                          double overallScore, double splitPenalty) {
            this.allocations = allocations;
            this.totalFulfilled = totalFulfilled;
            this.isPartialFulfillment = isPartialFulfillment;
            this.isMultiLocation = isMultiLocation;
            this.overallScore = overallScore;
            this.splitPenalty = splitPenalty;
        }
    }
    
    private enum SourcingStrategy {
        BATCH, SEQUENTIAL
    }
}