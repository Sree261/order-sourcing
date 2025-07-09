package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.OrderDTO;
import com.ordersourcing.engine.dto.SourcingResponse;
import com.ordersourcing.engine.dto.SimplifiedSourcingResponse;

public interface BatchSourcingService {
    
    /**
     * Main sourcing method with smart batch/sequential decision
     */
    SourcingResponse sourceOrder(OrderDTO order);
    
    /**
     * Simplified sourcing method that returns only essential fulfillment information
     */
    SimplifiedSourcingResponse sourceOrderSimplified(OrderDTO order);
}