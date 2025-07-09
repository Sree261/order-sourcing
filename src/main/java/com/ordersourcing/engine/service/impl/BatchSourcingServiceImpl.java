package com.ordersourcing.engine.service.impl;

import com.ordersourcing.engine.dto.*;
import com.ordersourcing.engine.model.*;
import com.ordersourcing.engine.repository.LocationRepository;
import com.ordersourcing.engine.repository.InventoryRepository;
import com.ordersourcing.engine.service.BatchSourcingService;
import com.ordersourcing.engine.service.LocationFilterExecutionService;
import com.ordersourcing.engine.service.InventoryApiService;
import com.ordersourcing.engine.service.PromiseDateService;
import com.ordersourcing.engine.service.ScoringConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private LocationRepository locationRepository;
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private ScoringConfigurationService scoringConfigurationService;
    
    // Configuration for batch vs sequential decision
    private static final int BATCH_THRESHOLD_ITEMS = 3;
    private static final int BATCH_THRESHOLD_TOTAL_QUANTITY = 10;
    
    /**
     * Main sourcing method with smart batch/sequential decision
     */
    public SourcingResponse sourceOrder(OrderDTO order) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Decide processing strategy
            SourcingStrategy strategy = decideSourcingStrategy(order);
            log.info("Using {} strategy for order: {} with {} items", 
                    strategy, order.getTempOrderId(), order.getOrderItems().size());
            
            SourcingResponse response;
            if (strategy == SourcingStrategy.BATCH) {
                response = batchSourceOrder(order, startTime);
            } else {
                response = sequentialSourceOrder(order, startTime);
            }
            
            // Add final metadata
            response.getMetadata().setProcesssingStrategy(strategy.name());
            response.getMetadata().setTotalProcessingTimeMs(System.currentTimeMillis() - startTime);
            response.getMetadata().setResponseTimestamp(LocalDateTime.now());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error in order sourcing for order: {}", order.getTempOrderId(), e);
            return createErrorResponse(order, e, System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * Simplified sourcing method that returns only essential fulfillment information
     */
    public SimplifiedSourcingResponse sourceOrderSimplified(OrderDTO order) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Use the existing detailed sourcing logic
            SourcingResponse detailedResponse = sourceOrder(order);
            
            // Convert to simplified response
            return convertToSimplifiedResponse(detailedResponse, startTime);
            
        } catch (Exception e) {
            log.error("Error in simplified order sourcing for order: {}", order.getTempOrderId(), e);
            return createErrorSimplifiedResponse(order, e, System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * Optimized batch processing for multiple items
     */
    private SourcingResponse batchSourceOrder(OrderDTO order, long startTime) {
        log.debug("Starting batch sourcing for order: {}", order.getTempOrderId());
        
        // Step 1: Group OrderItems by LocationFilter ID to eliminate duplicate executions
        Map<String, List<OrderItemDTO>> filterGroups = order.getOrderItems().stream()
                .collect(Collectors.groupingBy(OrderItemDTO::getLocationFilterId));
        
        log.debug("Grouped {} items into {} filter groups", order.getOrderItems().size(), filterGroups.size());
        
        // Step 2: Parallel filter execution (one per unique filter)
        long filterStartTime = System.currentTimeMillis();
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
            
            long filterTime = System.currentTimeMillis() - filterStartTime;
            
            // Step 4: Parallel promise date calculation
            long promiseDateStartTime = System.currentTimeMillis();
            CompletableFuture<Map<String, PromiseDateBreakdown>> promiseDateFuture = 
                    promiseDateService.batchCalculatePromiseDates(order.getOrderItems(), 
                            filterResults, inventoryResults, order);
            
            Map<String, PromiseDateBreakdown> promiseDateResults = promiseDateFuture.get();
            long promiseDateTime = System.currentTimeMillis() - promiseDateStartTime;
            
            // Step 5: Build fulfillment plans
            List<SourcingResponse.EnhancedFulfillmentPlan> fulfillmentPlans = 
                    buildFulfillmentPlans(order.getOrderItems(), filterResults, 
                            inventoryResults, promiseDateResults, order);
            
            // Build response with detailed metadata
            return SourcingResponse.builder()
                    .tempOrderId(order.getTempOrderId())
                    .fulfillmentPlans(fulfillmentPlans)
                    .metadata(SourcingResponse.SourcingMetadata.builder()
                            .filterExecutionTimeMs(filterTime)
                            .inventoryFetchTimeMs(filterTime) // Same as filter time since parallel
                            .promiseDateCalculationTimeMs(promiseDateTime)
                            .totalLocationsEvaluated(calculateTotalLocationsEvaluated(filterResults))
                            .filtersExecuted(filterGroups.size())
                            .cacheHitInfo(buildCacheHitInfo(filterResults))
                            .performanceWarnings(identifyPerformanceWarnings(filterTime, promiseDateTime))
                            .requestTimestamp(order.getRequestTimestamp())
                            .build())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error in batch processing", e);
            throw new RuntimeException("Batch processing failed", e);
        }
    }
    
    /**
     * Sequential processing for simple orders
     */
    private SourcingResponse sequentialSourceOrder(OrderDTO order, long startTime) {
        log.debug("Starting sequential sourcing for order: {}", order.getTempOrderId());
        
        List<SourcingResponse.EnhancedFulfillmentPlan> fulfillmentPlans = new ArrayList<>();
        long totalFilterTime = 0;
        long totalInventoryTime = 0;
        long totalPromiseDateTime = 0;
        int totalLocationsEvaluated = 0;
        
        for (OrderItemDTO orderItem : order.getOrderItems()) {
            try {
                // Filter execution
                long filterStart = System.currentTimeMillis();
                List<Location> locations = locationFilterService.executeLocationFilter(
                        orderItem.getLocationFilterId(), order);
                totalFilterTime += (System.currentTimeMillis() - filterStart);
                totalLocationsEvaluated += locations.size();
                
                if (locations.isEmpty()) {
                    log.warn("No locations found for item: {} with filter: {}", 
                            orderItem.getSku(), orderItem.getLocationFilterId());
                    continue;
                }
                
                // Inventory fetch
                long inventoryStart = System.currentTimeMillis();
                List<Inventory> inventories = inventoryApiService.fetchInventoryBySku(orderItem.getSku());
                totalInventoryTime += (System.currentTimeMillis() - inventoryStart);
                
                if (inventories.isEmpty()) {
                    log.warn("No inventory found for SKU: {}", orderItem.getSku());
                    continue;
                }
                
                // Find optimal fulfillment strategy
                FulfillmentStrategy strategy = findOptimalFulfillmentStrategy(
                        locations, inventories, orderItem, order);
                
                if (strategy != null) {
                    // Promise date calculation (use primary location for timing)
                    long promiseDateStart = System.currentTimeMillis();
                    LocationInventoryPair primaryPair = strategy.allocations.get(0);
                    PromiseDateBreakdown promiseDate = promiseDateService.calculateEnhancedPromiseDate(
                            orderItem, primaryPair.location, primaryPair.inventory, order);
                    totalPromiseDateTime += (System.currentTimeMillis() - promiseDateStart);
                    
                    // Only add fulfillment plan if promise date calculation was successful
                    if (promiseDate != null) {
                        // Build fulfillment plan
                        Map<String, PromiseDateBreakdown> promiseDateMap = Map.of(orderItem.getSku(), promiseDate);
                        SourcingResponse.EnhancedFulfillmentPlan plan = buildMultiLocationFulfillmentPlan(
                                orderItem, strategy, promiseDateMap, order);
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
        
        return SourcingResponse.builder()
                .tempOrderId(order.getTempOrderId())
                .fulfillmentPlans(fulfillmentPlans)
                .metadata(SourcingResponse.SourcingMetadata.builder()
                        .filterExecutionTimeMs(totalFilterTime)
                        .inventoryFetchTimeMs(totalInventoryTime)
                        .promiseDateCalculationTimeMs(totalPromiseDateTime)
                        .totalLocationsEvaluated(totalLocationsEvaluated)
                        .filtersExecuted(order.getOrderItems().size())
                        .cacheHitInfo(new HashMap<>())
                        .performanceWarnings(identifyPerformanceWarnings(totalFilterTime, totalPromiseDateTime))
                        .requestTimestamp(order.getRequestTimestamp())
                        .build())
                .build();
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
     * Build fulfillment plans from batch results
     */
    private List<SourcingResponse.EnhancedFulfillmentPlan> buildFulfillmentPlans(
            List<OrderItemDTO> orderItems,
            Map<String, List<Location>> filterResults,
            Map<String, List<Inventory>> inventoryResults,
            Map<String, PromiseDateBreakdown> promiseDateResults,
            OrderDTO order) {
        
        List<SourcingResponse.EnhancedFulfillmentPlan> plans = new ArrayList<>();
        
        for (OrderItemDTO orderItem : orderItems) {
            try {
                List<Location> locations = filterResults.get(orderItem.getLocationFilterId());
                List<Inventory> inventories = inventoryResults.get(orderItem.getSku());
                PromiseDateBreakdown promiseDate = promiseDateResults.get(orderItem.getSku());
                
                if (locations != null && !locations.isEmpty() && 
                    inventories != null && !inventories.isEmpty()) {
                    
                    FulfillmentStrategy strategy = findOptimalFulfillmentStrategy(
                            locations, inventories, orderItem, order);
                    
                    if (strategy != null) {
                        SourcingResponse.EnhancedFulfillmentPlan plan = buildMultiLocationFulfillmentPlan(
                                orderItem, strategy, promiseDateResults, order);
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
        
        // Calculate total value for this item
        double totalValue = orderItem.getUnitPrice() != null ? 
            orderItem.getUnitPrice() * orderItem.getQuantity() : 0.0;
        
        // Get scoring configuration for this order item
        var scoringConfig = scoringConfigurationService.getScoringConfigurationForItem(orderItem);
        
        // Use configurable scoring service
        double penalty = scoringConfigurationService.calculateSplitPenalty(
            locationCount, totalValue, scoringConfig, orderItem);
        
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
     * Build comprehensive item-centric fulfillment plan
     */
    private SourcingResponse.EnhancedFulfillmentPlan buildMultiLocationFulfillmentPlan(
            OrderItemDTO orderItem, FulfillmentStrategy strategy, 
            Map<String, PromiseDateBreakdown> promiseDateResults, OrderDTO order) {
        
        // Get ALL possible locations for this item (not just allocated ones)
        List<Location> allLocations = locationRepository.findAll();
        List<Inventory> allInventories = inventoryRepository.findBySkuAndQuantityGreaterThan(orderItem.getSku(), 0);
        
        // Build location fulfillments for ALL viable locations
        List<SourcingResponse.LocationFulfillment> locationFulfillments = new ArrayList<>();
        Map<Integer, LocationInventoryPair> allocatedMap = strategy.allocations.stream()
                .collect(Collectors.toMap(pair -> pair.location.getId(), pair -> pair));
        
        // Get all viable location-inventory pairs (same logic as in strategy evaluation)
        List<LocationInventoryPair> allViablePairs = new ArrayList<>();
        for (Location location : allLocations) {
            for (Inventory inventory : allInventories) {
                if (inventory.getLocationId().equals(location.getId())) {
                    double score = calculateLocationScore(location, inventory, orderItem);
                    allViablePairs.add(new LocationInventoryPair(location, inventory, score));
                }
            }
        }
        
        // Sort by score for priority assignment
        allViablePairs.sort((a, b) -> Double.compare(b.score, a.score));
        
        // Build location fulfillment entries
        for (int i = 0; i < allViablePairs.size(); i++) {
            LocationInventoryPair pair = allViablePairs.get(i);
            LocationInventoryPair allocatedPair = allocatedMap.get(pair.location.getId());
            
            boolean isAllocated = allocatedPair != null;
            boolean isPrimary = i == 0; // Best scoring location
            boolean canFulfillCompletely = pair.inventory.getQuantity() >= orderItem.getQuantity();
            
            String fulfillmentStatus;
            int allocatedQuantity = 0;
            if (isAllocated) {
                allocatedQuantity = allocatedPair.allocatedQuantity;
                fulfillmentStatus = (allocatedQuantity >= orderItem.getQuantity()) ? "FULL" : "PARTIAL";
            } else {
                fulfillmentStatus = "AVAILABLE_NOT_USED";
            }
            
            PromiseDateBreakdown promiseDate = promiseDateResults.get(orderItem.getSku());
            
            SourcingResponse.LocationFulfillment locationFulfillment = SourcingResponse.LocationFulfillment.builder()
                    .location(buildLocationInfo(pair.location, pair.inventory, order))
                    .availableInventory(pair.inventory.getQuantity())
                    .allocatedQuantity(allocatedQuantity)
                    .canFulfillCompletely(canFulfillCompletely)
                    .promiseDates(promiseDate)
                    .locationScore(pair.score)
                    .allocationPriority(i + 1)
                    .isPrimaryLocation(isPrimary)
                    .isAllocatedInOptimalPlan(isAllocated)
                    .fulfillmentStatus(fulfillmentStatus)
                    .warnings(identifyWarnings(pair.location, pair.inventory, orderItem))
                    .build();
            
            locationFulfillments.add(locationFulfillment);
        }
        
        // Build comprehensive warnings
        List<String> allWarnings = new ArrayList<>();
        if (strategy.isPartialFulfillment) {
            allWarnings.add(String.format("Partial fulfillment: %d of %d requested units", 
                                        strategy.totalFulfilled, orderItem.getQuantity()));
        }
        if (strategy.isMultiLocation) {
            allWarnings.add(String.format("Multi-location shipment across %d locations (penalty: %.1f points)", 
                                        strategy.allocations.size(), strategy.splitPenalty));
        }
        
        // Add location-specific warnings
        locationFulfillments.stream()
                .filter(lf -> lf.getIsAllocatedInOptimalPlan())
                .flatMap(lf -> lf.getWarnings().stream())
                .distinct()
                .forEach(allWarnings::add);
        
        return SourcingResponse.EnhancedFulfillmentPlan.builder()
                // Order Item Information
                .sku(orderItem.getSku())
                .requestedQuantity(orderItem.getQuantity())
                .deliveryType(orderItem.getDeliveryType())
                .locationFilterId(orderItem.getLocationFilterId())
                .unitPrice(orderItem.getUnitPrice())
                
                // Fulfillment Summary
                .totalFulfilled(strategy.totalFulfilled)
                .remainingQuantity(orderItem.getQuantity() - strategy.totalFulfilled)
                .isPartialFulfillment(strategy.isPartialFulfillment)
                .isMultiLocationFulfillment(strategy.isMultiLocation)
                .isBackorder(false)
                
                // All Location Options
                .locationFulfillments(locationFulfillments)
                
                // Optimization Results
                .overallScore(strategy.overallScore)
                .splitPenalty(strategy.splitPenalty)
                .recommendedStrategy(strategy.isMultiLocation ? "MULTI_LOCATION" : "SINGLE_LOCATION")
                .scoringFactors(buildScoringFactorsExplanation(strategy))
                .warnings(allWarnings)
                .build();
    }
    
    /**
     * Build location info from location and inventory
     */
    private SourcingResponse.LocationInfo buildLocationInfo(Location location, Inventory inventory, OrderDTO order) {
        return SourcingResponse.LocationInfo.builder()
                .id(location.getId())
                .name(location.getName())
                .type(determineLocationType(location))
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .transitTimeDays(location.getTransitTime())
                .processingTimeHours(inventory.getProcessingTime() * 24)
                .capabilities(Arrays.asList("STANDARD")) // Simplified
                .distanceFromCustomer(calculateDistance(location, order))
                .build();
    }
    
    
    /**
     * Build explanation of scoring factors
     */
    private String buildScoringFactorsExplanation(FulfillmentStrategy strategy) {
        StringBuilder sb = new StringBuilder();
        sb.append("Transit time, processing time, inventory availability");
        
        if (strategy.isMultiLocation) {
            sb.append(String.format("; Split penalty: %.1f points for %d locations", 
                                  strategy.splitPenalty, strategy.allocations.size()));
        }
        
        return sb.toString();
    }
    
    
    private String determineLocationType(Location location) {
        String name = location.getName().toLowerCase();
        if (name.contains("store")) return "STORE";
        if (name.contains("warehouse")) return "WAREHOUSE";
        return "DC";
    }
    
    private double calculateDistance(Location location, OrderDTO order) {
        return Math.sqrt(Math.pow(location.getLatitude() - order.getLatitude(), 2) + 
                        Math.pow(location.getLongitude() - order.getLongitude(), 2)) * 111.32;
    }
    
    private List<String> identifyWarnings(Location location, Inventory inventory, OrderItemDTO orderItem) {
        List<String> warnings = new ArrayList<>();
        
        if (inventory.getQuantity() < orderItem.getQuantity()) {
            warnings.add("Insufficient inventory - partial fulfillment");
        }
        
        if (location.getTransitTime() > 3) {
            warnings.add("Long transit time");
        }
        
        if ("SAME_DAY".equals(orderItem.getDeliveryType()) && location.getTransitTime() > 1) {
            warnings.add("Same day delivery may not be possible");
        }
        
        return warnings;
    }
    
    private Integer calculateTotalLocationsEvaluated(Map<String, List<Location>> filterResults) {
        return filterResults.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    private Map<String, String> buildCacheHitInfo(Map<String, List<Location>> filterResults) {
        Map<String, String> cacheInfo = new HashMap<>();
        cacheInfo.put("filterCacheHits", String.valueOf(filterResults.size()));
        return cacheInfo;
    }
    
    private List<String> identifyPerformanceWarnings(long filterTime, long promiseDateTime) {
        List<String> warnings = new ArrayList<>();
        
        if (filterTime > 200) {
            warnings.add("Filter execution exceeded 200ms");
        }
        
        if (promiseDateTime > 100) {
            warnings.add("Promise date calculation exceeded 100ms");
        }
        
        return warnings;
    }
    
    private SourcingResponse createErrorResponse(OrderDTO order, Exception e, long processingTime) {
        return SourcingResponse.builder()
                .tempOrderId(order.getTempOrderId())
                .fulfillmentPlans(Collections.emptyList())
                .metadata(SourcingResponse.SourcingMetadata.builder()
                        .totalProcessingTimeMs(processingTime)
                        .performanceWarnings(Arrays.asList("Error in sourcing: " + e.getMessage()))
                        .requestTimestamp(order.getRequestTimestamp())
                        .responseTimestamp(LocalDateTime.now())
                        .build())
                .build();
    }
    
    /**
     * Convert detailed response to simplified response
     */
    private SimplifiedSourcingResponse convertToSimplifiedResponse(SourcingResponse detailedResponse, long startTime) {
        List<SimplifiedSourcingResponse.FulfillmentPlan> simplifiedPlans = new ArrayList<>();
        
        for (SourcingResponse.EnhancedFulfillmentPlan enhancedPlan : detailedResponse.getFulfillmentPlans()) {
            List<SimplifiedSourcingResponse.LocationAllocation> locationAllocations = new ArrayList<>();
            
            // Only include locations that are actually allocated in the optimal plan
            for (SourcingResponse.LocationFulfillment locationFulfillment : enhancedPlan.getLocationFulfillments()) {
                if (locationFulfillment.getIsAllocatedInOptimalPlan() && 
                    locationFulfillment.getAllocatedQuantity() > 0 &&
                    locationFulfillment.getPromiseDates() != null) {
                    
                    SimplifiedSourcingResponse.DeliveryTiming deliveryTiming = 
                        SimplifiedSourcingResponse.DeliveryTiming.builder()
                            .estimatedShipDate(locationFulfillment.getPromiseDates().getCarrierPickupTime())
                            .estimatedDeliveryDate(locationFulfillment.getPromiseDates().getEstimatedDeliveryDate())
                            .transitTimeDays(locationFulfillment.getLocation().getTransitTimeDays())
                            .processingTimeHours(locationFulfillment.getLocation().getProcessingTimeHours())
                            .build();
                    
                    SimplifiedSourcingResponse.LocationAllocation allocation = 
                        SimplifiedSourcingResponse.LocationAllocation.builder()
                            .locationId(locationFulfillment.getLocation().getId())
                            .locationName(locationFulfillment.getLocation().getName())
                            .allocatedQuantity(locationFulfillment.getAllocatedQuantity())
                            .locationScore(locationFulfillment.getLocationScore())
                            .deliveryTiming(deliveryTiming)
                            .build();
                    
                    locationAllocations.add(allocation);
                }
            }
            
            SimplifiedSourcingResponse.FulfillmentPlan simplifiedPlan = 
                SimplifiedSourcingResponse.FulfillmentPlan.builder()
                    .sku(enhancedPlan.getSku())
                    .requestedQuantity(enhancedPlan.getRequestedQuantity())
                    .totalFulfilled(enhancedPlan.getTotalFulfilled())
                    .isPartialFulfillment(enhancedPlan.getIsPartialFulfillment())
                    .overallScore(enhancedPlan.getOverallScore())
                    .locationAllocations(locationAllocations)
                    .build();
            
            simplifiedPlans.add(simplifiedPlan);
        }
        
        return SimplifiedSourcingResponse.builder()
                .orderId(detailedResponse.getTempOrderId())
                .fulfillmentPlans(simplifiedPlans)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }
    
    /**
     * Create simplified error response
     */
    private SimplifiedSourcingResponse createErrorSimplifiedResponse(OrderDTO order, Exception e, long processingTime) {
        return SimplifiedSourcingResponse.builder()
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