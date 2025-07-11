package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.*;
import com.ordersourcing.engine.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface PromiseDateService {
    
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