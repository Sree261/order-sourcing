package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.OrderDTO;
import com.ordersourcing.engine.dto.SourcingResponse;

public interface BatchSourcingService {
    
    /**
     * Main sourcing method that returns essential fulfillment information
     */
    SourcingResponse sourceOrder(OrderDTO order);
}