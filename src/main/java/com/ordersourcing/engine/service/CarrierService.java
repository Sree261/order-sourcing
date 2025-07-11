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
}