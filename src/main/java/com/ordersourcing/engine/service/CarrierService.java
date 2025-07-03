package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.model.CarrierConfiguration;
import com.ordersourcing.engine.model.Location;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CarrierService {
    
    /**
     * Gets the best carrier configuration for a delivery type and location
     */
    Optional<CarrierConfiguration> getBestCarrierConfiguration(String deliveryType, 
                                                             Double distance, 
                                                             OrderItemDTO orderItem);
    
    /**
     * Calculates transit time considering carrier configuration and distance
     */
    Integer calculateTransitTime(CarrierConfiguration carrier, Location from, 
                              Double customerLat, Double customerLon, 
                              LocalDateTime orderTime, Boolean isPeakSeason);
    
    /**
     * Calculates carrier pickup time based on cutoff schedules
     */
    LocalDateTime calculatePickupTime(CarrierConfiguration carrier, LocalDateTime orderTime);
    
    /**
     * Gets estimated delivery window for a carrier
     */
    LocalDateTime[] getDeliveryWindow(CarrierConfiguration carrier, LocalDateTime deliveryDate);
    
    /**
     * Gets all available carriers for a delivery type with filtering
     */
    List<CarrierConfiguration> getAvailableCarriers(String deliveryType, 
                                                  Double distance, 
                                                  Boolean weekendRequired, 
                                                  OrderItemDTO orderItem);
    
    /**
     * Calculates carrier-specific service adjustments
     */
    Integer calculateServiceAdjustment(CarrierConfiguration carrier, String deliveryType);
}