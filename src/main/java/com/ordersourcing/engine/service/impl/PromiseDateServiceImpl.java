package com.ordersourcing.engine.service.impl;

import com.ordersourcing.engine.model.*;
import com.ordersourcing.engine.dto.*;
import com.ordersourcing.engine.service.CarrierService;
import com.ordersourcing.engine.service.PromiseDateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
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
     * Calculates promise dates for a fulfillment plan
     */
    public void calculatePromiseDates(FulfillmentPlan plan, OrderItem orderItem, Inventory inventory) {
        if (plan == null || orderItem == null || inventory == null || plan.getLocation() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String deliveryType = orderItem.getDeliveryType();
        Location location = plan.getLocation();

        // Store delivery type and time components
        plan.setDeliveryType(deliveryType);
        plan.setProcessingTimeHours(convertDaysToHours(inventory.getProcessingTime()));
        plan.setTransitTimeHours(convertDaysToHours(location.getTransitTime()));

        // Calculate estimated ship date (current time + processing time)
        LocalDateTime estimatedShipDate = calculateEstimatedShipDate(now, inventory.getProcessingTime());
        plan.setEstimatedShipDate(estimatedShipDate);

        // Calculate estimated delivery date (ship date + transit time)
        LocalDateTime estimatedDeliveryDate = calculateEstimatedDeliveryDate(estimatedShipDate, location.getTransitTime());
        plan.setEstimatedDeliveryDate(estimatedDeliveryDate);

        // Calculate promise date based on delivery type and add buffer
        LocalDateTime promiseDate = calculatePromiseDate(estimatedDeliveryDate, deliveryType);
        plan.setPromiseDate(promiseDate);
    }

    /**
     * Calculates estimated ship date considering business hours and weekends
     */
    private LocalDateTime calculateEstimatedShipDate(LocalDateTime currentTime, int processingTimeDays) {
        LocalDateTime shipDate = currentTime;

        // Add processing time
        shipDate = shipDate.plusDays(processingTimeDays);

        // Adjust for business hours (assume 9 AM - 6 PM)
        if (shipDate.getHour() < 9) {
            shipDate = shipDate.withHour(9).withMinute(0).withSecond(0);
        } else if (shipDate.getHour() >= 18) {
            shipDate = shipDate.plusDays(1).withHour(9).withMinute(0).withSecond(0);
        }

        // Skip weekends for shipping
        while (shipDate.getDayOfWeek() == DayOfWeek.SATURDAY || shipDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            shipDate = shipDate.plusDays(1);
        }

        return shipDate;
    }

    /**
     * Calculates estimated delivery date based on ship date and transit time
     */
    private LocalDateTime calculateEstimatedDeliveryDate(LocalDateTime shipDate, int transitTimeDays) {
        LocalDateTime deliveryDate = shipDate.plusDays(transitTimeDays);

        // For weekend deliveries, adjust based on delivery type
        // Most standard deliveries don't deliver on weekends
        if (deliveryDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
            deliveryDate = deliveryDate.plusDays(2); // Move to Monday
        } else if (deliveryDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            deliveryDate = deliveryDate.plusDays(1); // Move to Monday
        }

        return deliveryDate;
    }

    /**
     * Calculates promise date based on delivery type with appropriate buffers
     */
    private LocalDateTime calculatePromiseDate(LocalDateTime estimatedDeliveryDate, String deliveryType) {
        LocalDateTime promiseDate = estimatedDeliveryDate;

        switch (deliveryType.toUpperCase()) {
            case "SAME_DAY":
                // Same day delivery: promise by end of day
                promiseDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
                break;
            case "NEXT_DAY":
            case "EXPRESS":
                // Next day: add minimal buffer (4 hours)
                promiseDate = estimatedDeliveryDate.plusHours(4);
                break;
            case "SHIP_FROM_STORE":
                // Ship from store: add 1 day buffer
                promiseDate = estimatedDeliveryDate.plusDays(1);
                break;
            case "STANDARD":
            default:
                // Standard delivery: add 2 days buffer
                promiseDate = estimatedDeliveryDate.plusDays(2);
                break;
        }

        // Ensure promise date is not on weekend for most delivery types
        if (!deliveryType.equalsIgnoreCase("SAME_DAY")) {
            while (promiseDate.getDayOfWeek() == DayOfWeek.SATURDAY || promiseDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                promiseDate = promiseDate.plusDays(1);
            }
        }

        return promiseDate;
    }

    /**
     * Converts days to hours for storage
     */
    private Integer convertDaysToHours(int days) {
        return days * 24;
    }

    /**
     * Calculates promise date for a specific delivery type and location
     * This method can be used for quick promise date calculations without creating a full plan
     */
    public LocalDateTime calculatePromiseDateForDeliveryType(String deliveryType, Location location, Inventory inventory) {
        LocalDateTime now = LocalDateTime.now();
        
        LocalDateTime shipDate = calculateEstimatedShipDate(now, inventory.getProcessingTime());
        LocalDateTime deliveryDate = calculateEstimatedDeliveryDate(shipDate, location.getTransitTime());
        
        return calculatePromiseDate(deliveryDate, deliveryType);
    }

    /**
     * Checks if a promise date can be met for a given delivery type and location
     */
    public boolean canMeetPromiseDate(LocalDateTime requestedPromiseDate, String deliveryType, Location location, Inventory inventory) {
        LocalDateTime calculatedPromiseDate = calculatePromiseDateForDeliveryType(deliveryType, location, inventory);
        return calculatedPromiseDate.isBefore(requestedPromiseDate) || calculatedPromiseDate.isEqual(requestedPromiseDate);
    }

    /**
     * Gets the latest possible promise date for same day delivery
     */
    public LocalDateTime getLatestSameDayPromise() {
        return LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
    }

    /**
     * Calculates business days between two dates
     */
    public long calculateBusinessDaysBetween(LocalDateTime start, LocalDateTime end) {
        long totalDays = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
        long businessDays = 0;
        
        LocalDateTime current = start;
        for (int i = 0; i < totalDays; i++) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                businessDays++;
            }
            current = current.plusDays(1);
        }
        
        return businessDays;
    }

    /**
     * Calculates promise dates for individual quantities of an item
     * This method considers batch processing and potential delays for higher quantities
     */
    public List<Map<String, Object>> calculateQuantityPromiseDates(OrderItem orderItem, Location location, Inventory inventory) {
        List<Map<String, Object>> quantityPromises = new ArrayList<>();
        
        LocalDateTime basePromiseDate = calculatePromiseDateForDeliveryType(
            orderItem.getDeliveryType(), location, inventory);
        
        for (int quantity = 1; quantity <= orderItem.getQuantity(); quantity++) {
            Map<String, Object> quantityPromise = new HashMap<>();
            quantityPromise.put("quantity", quantity);
            
            // For larger quantities, add processing buffer
            LocalDateTime adjustedPromiseDate = adjustPromiseDateForQuantity(
                basePromiseDate, quantity, orderItem.getDeliveryType(), inventory);
            
            quantityPromise.put("promiseDate", adjustedPromiseDate);
            quantityPromise.put("estimatedShipDate", 
                calculateEstimatedShipDate(LocalDateTime.now(), inventory.getProcessingTime()));
            quantityPromise.put("estimatedDeliveryDate", 
                calculateEstimatedDeliveryDate(
                    (LocalDateTime) quantityPromise.get("estimatedShipDate"), 
                    location.getTransitTime()));
            
            quantityPromises.add(quantityPromise);
        }
        
        return quantityPromises;
    }

    /**
     * Adjusts promise date based on quantity considerations
     */
    private LocalDateTime adjustPromiseDateForQuantity(LocalDateTime basePromiseDate, int quantity, 
                                                      String deliveryType, Inventory inventory) {
        LocalDateTime adjustedDate = basePromiseDate;
        
        // For quantities above certain thresholds, add processing buffer
        if (quantity > 10) {
            // Large quantities may require additional processing time
            adjustedDate = adjustedDate.plusHours(12);
        } else if (quantity > 5) {
            // Medium quantities may require some additional time
            adjustedDate = adjustedDate.plusHours(6);
        }
        
        // Consider inventory availability
        if (inventory.getQuantity() < quantity) {
            // If inventory is insufficient, add backorder buffer
            adjustedDate = adjustedDate.plusDays(3);
        } else if (inventory.getQuantity() == quantity) {
            // If using all available inventory, add small buffer
            adjustedDate = adjustedDate.plusHours(4);
        }
        
        // Same day delivery cannot be extended beyond the day
        if ("SAME_DAY".equalsIgnoreCase(deliveryType)) {
            LocalDateTime sameDayLimit = getLatestSameDayPromise();
            if (adjustedDate.isAfter(sameDayLimit)) {
                // If we can't meet same day, move to next day
                adjustedDate = sameDayLimit.plusDays(1).withHour(17).withMinute(0).withSecond(0);
            }
        }
        
        return adjustedDate;
    }

    /**
     * Calculates staggered promise dates for multiple items with different quantities
     * This is useful when items may be processed in batches
     */
    public List<Map<String, Object>> calculateStaggeredPromiseDates(List<OrderItem> orderItems, 
                                                                   List<Location> locations, 
                                                                   List<Inventory> inventories) {
        List<Map<String, Object>> staggeredPromises = new ArrayList<>();
        LocalDateTime currentProcessingTime = LocalDateTime.now();
        
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            Location location = locations.get(i);
            Inventory inventory = inventories.get(i);
            
            Map<String, Object> itemPromise = new HashMap<>();
            itemPromise.put("sku", item.getSku());
            itemPromise.put("deliveryType", item.getDeliveryType());
            
            // Calculate cumulative processing time for staggered fulfillment
            LocalDateTime itemShipDate = calculateEstimatedShipDate(currentProcessingTime, inventory.getProcessingTime());
            LocalDateTime itemDeliveryDate = calculateEstimatedDeliveryDate(itemShipDate, location.getTransitTime());
            LocalDateTime itemPromiseDate = calculatePromiseDate(itemDeliveryDate, item.getDeliveryType());
            
            itemPromise.put("promiseDate", itemPromiseDate);
            itemPromise.put("estimatedShipDate", itemShipDate);
            itemPromise.put("estimatedDeliveryDate", itemDeliveryDate);
            
            // Calculate individual quantity promises for this item
            List<Map<String, Object>> quantityPromises = calculateQuantityPromiseDates(item, location, inventory);
            itemPromise.put("quantityPromises", quantityPromises);
            
            staggeredPromises.add(itemPromise);
            
            // Update current processing time for next item
            currentProcessingTime = itemShipDate.plusHours(2); // 2-hour buffer between items
        }
        
        return staggeredPromises;
    }

    /**
     * Enhanced promise date calculation with all factors
     */
    public PromiseDateBreakdown calculateEnhancedPromiseDate(OrderItemDTO orderItem, Location location, 
                                                           Inventory inventory, OrderDTO orderContext) {
        long startTime = System.currentTimeMillis();
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Step 1: System processing time (order validation, payment, etc.)
            LocalDateTime systemProcessingComplete = calculateSystemProcessingTime(now, orderItem, orderContext);
            
            // Step 2: Location processing time (warehouse/store specific)
            LocalDateTime locationProcessingComplete = calculateLocationProcessingTime(
                    systemProcessingComplete, location, inventory, orderItem);
            
            // Step 3: Get best carrier configuration
            double distance = calculateDistance(location, orderContext.getLatitude(), orderContext.getLongitude());
            Optional<CarrierConfiguration> carrierOpt = carrierService.getBestCarrierConfiguration(
                    orderItem.getDeliveryType(), distance, orderItem);
            
            if (carrierOpt.isEmpty()) {
                log.warn("No carrier found for delivery type: {} at distance: {} km", orderItem.getDeliveryType(), distance);
                return null; // Signal that this delivery mode is not feasible
            }
            
            CarrierConfiguration carrier = carrierOpt.get();
            
            // Step 4: Carrier pickup time
            LocalDateTime carrierPickupTime = carrierService.calculatePickupTime(carrier, locationProcessingComplete);
            
            // Step 5: Transit time calculation
            Integer transitHours = carrierService.calculateTransitTime(carrier, location, 
                    orderContext.getLatitude(), orderContext.getLongitude(), 
                    carrierPickupTime, orderContext.getIsPeakSeason()) * 24;
            
            // Step 6: Estimated delivery date
            LocalDateTime estimatedDeliveryDate = carrierPickupTime.plusHours(transitHours);
            
            // Step 7: Apply business day and weekend adjustments
            estimatedDeliveryDate = adjustForBusinessDays(estimatedDeliveryDate, carrier);
            
            // Step 8: Add buffer and calculate final promise date
            LocalDateTime promiseDate = calculateFinalPromiseDate(estimatedDeliveryDate, orderItem.getDeliveryType(), carrier);
            
            // Step 9: Apply weather and seasonal adjustments
            promiseDate = applyEnvironmentalAdjustments(promiseDate, orderContext, orderItem.getDeliveryType());
            
            // Build detailed breakdown
            return PromiseDateBreakdown.builder()
                    .promiseDate(promiseDate)
                    .orderProcessingStart(now)
                    .orderProcessingComplete(systemProcessingComplete)
                    .locationProcessingStart(systemProcessingComplete)
                    .locationProcessingComplete(locationProcessingComplete)
                    .carrierPickupTime(carrierPickupTime)
                    .estimatedDeliveryDate(estimatedDeliveryDate)
                    .systemProcessingHours((int) ChronoUnit.HOURS.between(now, systemProcessingComplete))
                    .locationProcessingHours((int) ChronoUnit.HOURS.between(systemProcessingComplete, locationProcessingComplete))
                    .carrierTransitHours(transitHours)
                    .bufferHours((int) ChronoUnit.HOURS.between(estimatedDeliveryDate, promiseDate))
                    .carrierCode(carrier.getCarrierCode())
                    .serviceLevel(carrier.getServiceLevel())
                    .deliveryType(orderItem.getDeliveryType())
                    .isBusinessDaysOnly(!carrier.getWeekendDelivery())
                    .isWeatherAdjusted(isWeatherSeason())
                    .isPeakSeasonAdjusted(orderContext.getIsPeakSeason())
                    .confidenceScore(calculateConfidenceScore(carrier, orderItem, orderContext))
                    .riskFactors(identifyRiskFactors(carrier, orderItem, orderContext))
                    .earliestPossibleDate(estimatedDeliveryDate)
                    .latestAcceptableDate(promiseDate.plusDays(1))
                    .calculationTimeMs(System.currentTimeMillis() - startTime)
                    .calculationMethod("COMPUTED")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error calculating enhanced promise date for item: {}", orderItem.getSku(), e);
            return PromiseDateBreakdown.createFallback(
                    LocalDateTime.now().plusDays(5), "Calculation error: " + e.getMessage());
        }
    }
    
    /**
     * Batch promise date calculation for multiple items
     */
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
    
    private LocalDateTime calculateSystemProcessingTime(LocalDateTime orderTime, OrderItemDTO orderItem, OrderDTO orderContext) {
        int processingHours = 1; // Base system processing time
        
        // High-value orders need additional verification
        if (orderContext.isHighValueOrder()) {
            processingHours += 2;
        }
        
        // Complex orders need more processing
        if (orderContext.isLargeOrder()) {
            processingHours += 1;
        }
        
        // Hazmat items need special processing
        if (orderItem.getIsHazmat() != null && orderItem.getIsHazmat()) {
            processingHours += 4;
        }
        
        // Express orders get priority processing
        if (orderItem.getIsExpressPriority() != null && orderItem.getIsExpressPriority()) {
            processingHours = Math.max(1, processingHours - 1);
        }
        
        return orderTime.plusHours(processingHours);
    }
    
    private LocalDateTime calculateLocationProcessingTime(LocalDateTime startTime, Location location, 
                                                        Inventory inventory, OrderItemDTO orderItem) {
        int processingHours = inventory.getProcessingTime() * 24; // Convert days to hours
        
        // Store vs warehouse processing differences
        if (location.getName().toLowerCase().contains("store")) {
            processingHours += 2; // Stores typically need more time
        }
        
        // Cold storage items need special handling
        if (orderItem.getRequiresColdStorage() != null && orderItem.getRequiresColdStorage()) {
            processingHours += 4;
        }
        
        // Large quantities need more processing time
        if (orderItem.getQuantity() > 10) {
            processingHours += (orderItem.getQuantity() / 10) * 2;
        }
        
        return startTime.plusHours(processingHours);
    }
    
    private LocalDateTime adjustForBusinessDays(LocalDateTime dateTime, CarrierConfiguration carrier) {
        if (carrier.getWeekendDelivery()) {
            return dateTime; // No adjustment needed
        }
        
        // Skip weekends
        while (dateTime.getDayOfWeek() == DayOfWeek.SATURDAY || 
               dateTime.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dateTime = dateTime.plusDays(1);
        }
        
        return dateTime;
    }
    
    private LocalDateTime calculateFinalPromiseDate(LocalDateTime estimatedDelivery, String deliveryType, 
                                                  CarrierConfiguration carrier) {
        LocalDateTime promiseDate = estimatedDelivery;
        
        // Add delivery-type specific buffers
        switch (deliveryType.toUpperCase()) {
            case "SAME_DAY":
                // Minimal buffer for same day
                promiseDate = promiseDate.plusHours(2);
                break;
            case "NEXT_DAY":
            case "EXPRESS":
                // Small buffer for express
                promiseDate = promiseDate.plusHours(6);
                break;
            case "STANDARD":
                // Standard buffer
                promiseDate = promiseDate.plusDays(1);
                break;
            default:
                promiseDate = promiseDate.plusDays(2);
        }
        
        // Carrier reliability buffer
        if (carrier.getOnTimePerformance() < 0.9) {
            promiseDate = promiseDate.plusHours(12);
        }
        
        return promiseDate;
    }
    
    private LocalDateTime applyEnvironmentalAdjustments(LocalDateTime promiseDate, OrderDTO orderContext, String deliveryType) {
        // Peak season adjustments
        if (orderContext.getIsPeakSeason() != null && orderContext.getIsPeakSeason()) {
            if ("SAME_DAY".equals(deliveryType)) {
                promiseDate = promiseDate.plusHours(4);
            } else {
                promiseDate = promiseDate.plusDays(1);
            }
        }
        
        // Weather adjustments (simplified - in real implementation, would integrate with weather APIs)
        if (isWeatherSeason()) {
            promiseDate = promiseDate.plusHours(6);
        }
        
        return promiseDate;
    }
    
    private double calculateDistance(Location location, Double customerLat, Double customerLon) {
        return Math.sqrt(Math.pow(location.getLatitude() - customerLat, 2) + 
                        Math.pow(location.getLongitude() - customerLon, 2)) * 111.32; // Approximate km
    }
    
    private Double calculateConfidenceScore(CarrierConfiguration carrier, OrderItemDTO orderItem, OrderDTO orderContext) {
        double score = 0.8; // Base confidence
        
        // Carrier performance impact
        if (carrier.getOnTimePerformance() != null) {
            score *= carrier.getOnTimePerformance();
        }
        
        // Peak season impact
        if (orderContext.getIsPeakSeason() != null && orderContext.getIsPeakSeason()) {
            score *= 0.9;
        }
        
        // Weather impact
        if (isWeatherSeason()) {
            score *= 0.95;
        }
        
        // Special handling impact
        if (orderItem.getIsHazmat() != null && orderItem.getIsHazmat()) {
            score *= 0.85;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    private String identifyRiskFactors(CarrierConfiguration carrier, OrderItemDTO orderItem, OrderDTO orderContext) {
        List<String> risks = new ArrayList<>();
        
        if (carrier.getOnTimePerformance() < 0.9) {
            risks.add("Carrier reliability below 90%");
        }
        
        if (orderContext.getIsPeakSeason() != null && orderContext.getIsPeakSeason()) {
            risks.add("Peak season delays possible");
        }
        
        if (isWeatherSeason()) {
            risks.add("Weather-related delays possible");
        }
        
        if (orderItem.getIsHazmat() != null && orderItem.getIsHazmat()) {
            risks.add("Hazmat processing delays");
        }
        
        if (orderContext.isLargeOrder()) {
            risks.add("Large order processing complexity");
        }
        
        return String.join(", ", risks);
    }
    
    private boolean isWeatherSeason() {
        // Simplified weather check - in real implementation, would integrate with weather APIs
        int month = LocalDateTime.now().getMonthValue();
        return month == 12 || month == 1 || month == 2 || (month >= 6 && month <= 8); // Winter or summer extremes
    }
}