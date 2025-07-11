package com.ordersourcing.engine.service.impl;

import com.ordersourcing.engine.model.*;
import com.ordersourcing.engine.dto.*;
import com.ordersourcing.engine.service.CarrierService;
import com.ordersourcing.engine.service.PromiseDateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PromiseDateServiceImpl implements PromiseDateService {

    @Autowired
    private CarrierService carrierService;

    /**
     * Simplified promise date calculation: processing time + transit time
     */
    @Override
    public PromiseDateBreakdown calculateEnhancedPromiseDate(OrderItemDTO orderItem, Location location, 
                                                           Inventory inventory, OrderDTO orderContext) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Step 1: Get carrier configuration for this delivery type
            double distance = calculateDistance(location, orderContext.getLatitude(), orderContext.getLongitude());
            Optional<CarrierConfiguration> carrierOpt = carrierService.getBestCarrierConfiguration(
                    orderItem.getDeliveryType(), distance, orderItem);
            
            if (carrierOpt.isEmpty()) {
                log.warn("No carrier found for delivery type: {} at distance: {} km", orderItem.getDeliveryType(), distance);
                return null; // Signal that this delivery mode is not feasible
            }
            
            CarrierConfiguration carrier = carrierOpt.get();
            
            // Step 2: Calculate processing time (inventory processing time in hours)
            int processingHours = inventory.getProcessingTime() * 24; // Convert days to hours
            LocalDateTime processingComplete = now.plusHours(processingHours);
            
            // Step 3: Calculate transit time (carrier base transit time in hours)
            int transitHours = carrier.getBaseTransitDays() * 24; // Convert days to hours
            LocalDateTime estimatedDeliveryDate = processingComplete.plusHours(transitHours);
            
            // Step 4: Promise date = delivery date (no additional buffer)

            // Build simple breakdown
            return PromiseDateBreakdown.builder()
                    .promiseDate(estimatedDeliveryDate)
                    .carrierPickupTime(processingComplete)
                    .estimatedDeliveryDate(estimatedDeliveryDate)
                    .locationProcessingHours(processingHours)
                    .carrierTransitHours(transitHours)
                    .carrierCode(carrier.getCarrierCode())
                    .serviceLevel(carrier.getServiceLevel())
                    .deliveryType(orderItem.getDeliveryType())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error calculating promise date for item: {}", orderItem.getSku(), e);
            return null; // Return null instead of fallback for failed calculations
        }
    }
    
    /**
     * Batch promise date calculation for multiple items
     */
    @Override
    public CompletableFuture<Map<String, PromiseDateBreakdown>> batchCalculatePromiseDates(
            List<OrderItemDTO> orderItems, Map<String, List<Location>> filterResults, 
            Map<String, List<Inventory>> inventoryResults, OrderDTO orderContext) {
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, PromiseDateBreakdown> results = new HashMap<>();
            
            orderItems.parallelStream().forEach(orderItem -> {
                try {
                    List<Location> eligibleLocations = filterResults.get(orderItem.getLocationFilterId());
                    List<Inventory> inventories = inventoryResults.get(orderItem.getSku());
                    
                    if (eligibleLocations != null && !eligibleLocations.isEmpty() && 
                        inventories != null && !inventories.isEmpty()) {
                        
                        // Find best location-inventory combination
                        for (Location location : eligibleLocations) {
                            Optional<Inventory> inventoryOpt = inventories.stream()
                                    .filter(inv -> inv.getLocationId().equals(location.getId()))
                                    .findFirst();
                            
                            if (inventoryOpt.isPresent()) {
                                PromiseDateBreakdown breakdown = calculateEnhancedPromiseDate(
                                        orderItem, location, inventoryOpt.get(), orderContext);
                                if (breakdown != null) {
                                    results.put(orderItem.getSku(), breakdown);
                                    break;
                                }
                                // Continue to next location if this delivery type is not feasible
                            }
                        }
                    }
                    
                    // No fallback needed - if no suitable combination found, item won't be in results
                    // This will result in an empty fulfillment plan for that delivery type
                    
                } catch (Exception e) {
                    log.error("Error in batch promise date calculation for SKU: {}, excluding from fulfillment", orderItem.getSku(), e);
                    // Item will be excluded from results, resulting in empty fulfillment plan
                }
            });
            
            return results;
        });
    }
    
    
    private double calculateDistance(Location location, Double customerLat, Double customerLon) {
        return Math.sqrt(Math.pow(location.getLatitude() - customerLat, 2) + 
                        Math.pow(location.getLongitude() - customerLon, 2)) * 111.32; // Approximate km
    }
    
}