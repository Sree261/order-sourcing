package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.model.Inventory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface InventoryApiService {
    
    /**
     * Batch fetch inventory for multiple SKUs from database
     */
    CompletableFuture<Map<String, List<Inventory>>> batchFetchInventory(List<OrderItemDTO> orderItems);
    
    /**
     * Fetch inventory for a single SKU from database with caching
     */
    List<Inventory> fetchInventoryBySku(String sku);
}