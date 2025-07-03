package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.*;
import com.ordersourcing.engine.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface PromiseDateService {
    
    /**
     * Calculates promise dates for a fulfillment plan
     */
    void calculatePromiseDates(FulfillmentPlan plan, OrderItem orderItem, Inventory inventory);
    
    /**
     * Calculates promise date for a specific delivery type and location
     * This method can be used for quick promise date calculations without creating a full plan
     */
    LocalDateTime calculatePromiseDateForDeliveryType(String deliveryType, Location location, Inventory inventory);
    
    /**
     * Checks if a promise date can be met for a given delivery type and location
     */
    boolean canMeetPromiseDate(LocalDateTime requestedPromiseDate, String deliveryType, Location location, Inventory inventory);
    
    /**
     * Gets the latest possible promise date for same day delivery
     */
    LocalDateTime getLatestSameDayPromise();
    
    /**
     * Calculates business days between two dates
     */
    long calculateBusinessDaysBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Calculates promise dates for individual quantities of an item
     * This method considers batch processing and potential delays for higher quantities
     */
    List<Map<String, Object>> calculateQuantityPromiseDates(OrderItem orderItem, Location location, Inventory inventory);
    
    /**
     * Calculates staggered promise dates for multiple items with different quantities
     * This is useful when items may be processed in batches
     */
    List<Map<String, Object>> calculateStaggeredPromiseDates(List<OrderItem> orderItems, 
                                                           List<Location> locations, 
                                                           List<Inventory> inventories);
    
    /**
     * Enhanced promise date calculation with all factors
     */
    PromiseDateBreakdown calculateEnhancedPromiseDate(OrderItemDTO orderItem, Location location, 
                                                    Inventory inventory, OrderDTO orderContext);
    
    /**
     * Batch promise date calculation for multiple items
     */
    CompletableFuture<Map<String, PromiseDateBreakdown>> batchCalculatePromiseDates(
            List<OrderItemDTO> orderItems, Map<String, List<Location>> filterResults, 
            Map<String, List<Inventory>> inventoryResults, OrderDTO orderContext);
}