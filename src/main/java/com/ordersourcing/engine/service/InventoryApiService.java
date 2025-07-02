package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.model.Inventory;
import com.ordersourcing.engine.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InventoryApiService {
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    public InventoryApiService() {
    }
    
    /**
     * Batch fetch inventory for multiple SKUs from database
     */
    public CompletableFuture<Map<String, List<Inventory>>> batchFetchInventory(List<OrderItemDTO> orderItems) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Extract unique SKUs
                List<String> skus = orderItems.stream()
                        .map(OrderItemDTO::getSku)
                        .distinct()
                        .collect(Collectors.toList());
                
                log.debug("Batch fetching inventory for {} SKUs from database", skus.size());
                
                // Use batch query to fetch all inventory records at once
                List<Inventory> allInventories = inventoryRepository.findBySkusWithStock(skus);
                
                // Group by SKU
                Map<String, List<Inventory>> results = allInventories.stream()
                        .collect(Collectors.groupingBy(Inventory::getSku));
                
                // Ensure all requested SKUs are in the results (even if empty)
                for (String sku : skus) {
                    results.putIfAbsent(sku, Collections.emptyList());
                }
                
                log.debug("Found inventory for {} out of {} requested SKUs", 
                         results.entrySet().stream().mapToInt(e -> e.getValue().isEmpty() ? 0 : 1).sum(), 
                         skus.size());
                
                return results;
                
            } catch (Exception e) {
                log.error("Error in batch inventory fetch from database", e);
                return new HashMap<>();
            }
        });
    }
    
    /**
     * Fetch inventory for a single SKU from database with caching
     */
    @Cacheable(value = "inventory", key = "#sku", unless = "#result == null")
    public List<Inventory> fetchInventoryBySku(String sku) {
        try {
            log.debug("Fetching inventory for SKU: {} from database", sku);
            List<Inventory> inventories = inventoryRepository.findBySkuAndQuantityGreaterThan(sku, 0);
            
            if (inventories.isEmpty()) {
                log.warn("No inventory found for SKU: {}", sku);
                return Collections.emptyList();
            }
            
            log.debug("Found {} inventory records for SKU: {}", inventories.size(), sku);
            return inventories;
            
        } catch (Exception e) {
            log.error("Error fetching inventory for SKU: {} from database", sku, e);
            return Collections.emptyList();
        }
    }
    
}
