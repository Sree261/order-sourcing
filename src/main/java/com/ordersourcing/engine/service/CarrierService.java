package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.model.CarrierConfiguration;
import com.ordersourcing.engine.model.Location;
import com.ordersourcing.engine.repository.CarrierConfigurationRepository;
import com.ordersourcing.engine.util.GeoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CarrierService {
    
    @Autowired
    private CarrierConfigurationRepository carrierConfigurationRepository;
    
    /**
     * Gets the best carrier configuration for a delivery type and location
     */
    @Cacheable(value = "carrierConfigs", key = "#deliveryType + ':' + #distance")
    public Optional<CarrierConfiguration> getBestCarrierConfiguration(String deliveryType, 
                                                                     Double distance, 
                                                                     OrderItemDTO orderItem) {
        List<CarrierConfiguration> carriers = carrierConfigurationRepository
                .findByDeliveryTypeAndMaxDistance(deliveryType, distance);
        
        return carriers.stream()
                .filter(carrier -> isCarrierSuitableForItem(carrier, orderItem))
                .findFirst(); // Already ordered by priority
    }
    
    /**
     * Calculates transit time considering carrier configuration and distance
     */
    public Integer calculateTransitTime(CarrierConfiguration carrier, Location from, 
                                      Double customerLat, Double customerLon, 
                                      LocalDateTime orderTime, Boolean isPeakSeason) {
        if (carrier == null) {
            return 2; // Default fallback
        }
        
        // Calculate distance for distance-based adjustments
        double distance = GeoUtils.calculateDistance(
                from.getLatitude(), from.getLongitude(), customerLat, customerLon);
        
        // Base transit time
        int transitDays = carrier.getBaseTransitDays();
        
        // Distance-based adjustment
        if (distance > 500) { // Long distance
            transitDays += Math.ceil(distance / 1000.0); // Add 1 day per 1000km
        }
        
        // Apply carrier multiplier
        transitDays = (int) Math.ceil(transitDays * carrier.getTransitTimeMultiplier());
        
        // Peak season adjustment
        if (isPeakSeason != null && isPeakSeason && carrier.getPeakSeasonDelayDays() > 0) {
            transitDays += carrier.getPeakSeasonDelayDays();
        }
        
        // Weekend service adjustment
        if (!carrier.getWeekendDelivery() && isWeekend(orderTime.plusDays(transitDays))) {
            transitDays += 2; // Skip weekend
        }
        
        // Apply performance adjustments based on on-time performance
        if (carrier.getOnTimePerformance() < 0.9) {
            transitDays += 1; // Add buffer for unreliable carriers
        }
        
        return Math.min(transitDays, carrier.getMaxTransitDays());
    }
    
    /**
     * Calculates carrier pickup time based on cutoff schedules
     */
    public LocalDateTime calculatePickupTime(CarrierConfiguration carrier, LocalDateTime orderTime) {
        if (carrier == null || carrier.getPickupCutoffTime() == null) {
            return orderTime.plusHours(2); // Default 2-hour pickup
        }
        
        LocalDateTime pickupTime = orderTime;
        LocalTime cutoff = carrier.getPickupCutoffTime();
        
        // Check if order is placed before cutoff today
        if (orderTime.toLocalTime().isBefore(cutoff)) {
            // Same day pickup
            pickupTime = orderTime.withHour(cutoff.getHour()).withMinute(cutoff.getMinute());
        } else {
            // Next day pickup
            pickupTime = orderTime.plusDays(1).withHour(cutoff.getHour()).withMinute(cutoff.getMinute());
        }
        
        // Skip weekends if carrier doesn't support weekend pickup
        if (!carrier.getWeekendPickup()) {
            while (isWeekend(pickupTime)) {
                pickupTime = pickupTime.plusDays(1);
            }
        }
        
        return pickupTime;
    }
    
    /**
     * Gets estimated delivery window for a carrier
     */
    public LocalDateTime[] getDeliveryWindow(CarrierConfiguration carrier, LocalDateTime deliveryDate) {
        if (carrier == null) {
            return new LocalDateTime[]{
                    deliveryDate.withHour(9).withMinute(0),
                    deliveryDate.withHour(18).withMinute(0)
            };
        }
        
        LocalTime startTime = carrier.getDeliveryStartTime() != null ? 
                carrier.getDeliveryStartTime() : LocalTime.of(9, 0);
        LocalTime endTime = carrier.getDeliveryEndTime() != null ? 
                carrier.getDeliveryEndTime() : LocalTime.of(18, 0);
        
        return new LocalDateTime[]{
                deliveryDate.withHour(startTime.getHour()).withMinute(startTime.getMinute()),
                deliveryDate.withHour(endTime.getHour()).withMinute(endTime.getMinute())
        };
    }
    
    /**
     * Checks if carrier supports special handling requirements
     */
    private boolean isCarrierSuitableForItem(CarrierConfiguration carrier, OrderItemDTO orderItem) {
        // Check hazmat requirements
        if (orderItem.getIsHazmat() != null && orderItem.getIsHazmat() && !carrier.getSupportsHazmat()) {
            return false;
        }
        
        // Check cold storage requirements
        if (orderItem.getRequiresColdStorage() != null && orderItem.getRequiresColdStorage() 
                && !carrier.getSupportsColdChain()) {
            return false;
        }
        
        // Check high value requirements
        if (orderItem.requiresHighSecurity() && !carrier.getSupportsHighValue()) {
            return false;
        }
        
        // Check value limits
        if (carrier.getMaxValueLimit() != null && orderItem.getUnitPrice() != null 
                && orderItem.getUnitPrice() > carrier.getMaxValueLimit()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets all available carriers for a delivery type with filtering
     */
    public List<CarrierConfiguration> getAvailableCarriers(String deliveryType, 
                                                          Double distance, 
                                                          Boolean weekendRequired, 
                                                          OrderItemDTO orderItem) {
        List<CarrierConfiguration> carriers;
        
        if (weekendRequired != null && weekendRequired) {
            carriers = carrierConfigurationRepository.findByDeliveryTypeAndWeekendService(deliveryType, true);
        } else {
            carriers = carrierConfigurationRepository.findByDeliveryTypeAndMaxDistance(deliveryType, distance);
        }
        
        return carriers.stream()
                .filter(carrier -> isCarrierSuitableForItem(carrier, orderItem))
                .toList();
    }
    
    /**
     * Calculates carrier-specific service adjustments
     */
    public Integer calculateServiceAdjustment(CarrierConfiguration carrier, String deliveryType) {
        int adjustment = 0;
        
        // Same day delivery adjustments
        if ("SAME_DAY".equals(deliveryType)) {
            if (carrier.getPickupCutoffTime() != null && 
                LocalTime.now().isAfter(carrier.getPickupCutoffTime().minusHours(2))) {
                adjustment += 24; // Move to next day if too late
            }
        }
        
        // Express delivery adjustments
        if ("NEXT_DAY".equals(deliveryType) || "EXPRESS".equals(deliveryType)) {
            if (carrier.getOnTimePerformance() < 0.95) {
                adjustment += 12; // Add buffer for less reliable express service
            }
        }
        
        return adjustment;
    }
    
    private boolean isWeekend(LocalDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}