package com.ordersourcing.engine.service.impl;

import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.model.CarrierConfiguration;
import com.ordersourcing.engine.repository.CarrierConfigurationRepository;
import com.ordersourcing.engine.service.CarrierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CarrierServiceImpl implements CarrierService {
    
    @Autowired
    private CarrierConfigurationRepository carrierConfigurationRepository;
    
    /**
     * Gets the best carrier configuration for a delivery type and location
     */
    @Cacheable(value = "carrierConfigs", key = "#deliveryType + ':' + #distance")
    @Override
    public Optional<CarrierConfiguration> getBestCarrierConfiguration(String deliveryType, 
                                                                     Double distance, 
                                                                     OrderItemDTO orderItem) {
        log.debug("Finding carrier for delivery type: {}, distance: {} km", deliveryType, distance);
        
        List<CarrierConfiguration> carriers = carrierConfigurationRepository
                .findByDeliveryTypeAndMaxDistance(deliveryType, distance);
        
        log.debug("Found {} carriers for delivery type: {}", carriers.size(), deliveryType);
        
        Optional<CarrierConfiguration> result = carriers.stream()
                .filter(carrier -> {
                    boolean suitable = isCarrierSuitableForItem(carrier, orderItem);
                    if (!suitable) {
                        log.debug("Carrier {} not suitable for item - hazmat: {}, cold: {}, high value: {}", 
                                carrier.getCarrierCode(), 
                                orderItem.getIsHazmat(),
                                orderItem.getRequiresColdStorage(),
                                orderItem.requiresHighSecurity());
                    }
                    return suitable;
                })
                .findFirst(); // Already ordered by priority
        
        if (result.isEmpty()) {
            log.warn("No suitable carrier found for delivery type: {} at distance: {} km", deliveryType, distance);
        } else {
            log.debug("Selected carrier: {} for delivery type: {}", result.get().getCarrierCode(), deliveryType);
        }
        
        return result;
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
        return !orderItem.requiresHighSecurity() || carrier.getSupportsHighValue();
    }
}