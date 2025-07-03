package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.OrderDTO;
import com.ordersourcing.engine.dto.SourcingResponse;

public interface BatchSourcingService {
    
    /**
     * Main sourcing method with smart batch/sequential decision
     */
    SourcingResponse sourceOrder(OrderDTO order);
}