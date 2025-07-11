package com.ordersourcing.engine.controller;

import com.ordersourcing.engine.service.BatchSourcingService;
import com.ordersourcing.engine.dto.OrderDTO;
import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.dto.SourcingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sourcing")
@Slf4j
public class SourcingController {

    @Autowired
    private BatchSourcingService batchSourcingService;

    @PostMapping("/source")
    public ResponseEntity<SourcingResponse> sourceOrder(@RequestBody @Valid OrderDTO orderDTO) {
        try {
            log.info("Received sourcing request for order: {} with {} items", 
                    orderDTO.getTempOrderId(), orderDTO.getOrderItems().size());
            
            // Validate order items have location filter IDs
            for (OrderItemDTO item : orderDTO.getOrderItems()) {
                if (item.getLocationFilterId() == null || item.getLocationFilterId().trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(createErrorSourcingResponse(orderDTO, 
                                    "Missing location filter ID for item: " + item.getSku()));
                }
            }
            
            // Execute sourcing
            SourcingResponse response = batchSourcingService.sourceOrder(orderDTO);
            
            // Log completion
            log.info("Completed sourcing for order: {} in {}ms", 
                    orderDTO.getTempOrderId(), response.getProcessingTimeMs());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for sourcing: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorSourcingResponse(orderDTO, "Invalid request: " + e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error in sourcing for order: {}", 
                    orderDTO != null ? orderDTO.getTempOrderId() : "unknown", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorSourcingResponse(orderDTO, "Internal error: " + e.getMessage()));
        }
    }
    
    private SourcingResponse createErrorSourcingResponse(OrderDTO orderDTO, String errorMessage) {
        return SourcingResponse.builder()
                .orderId(orderDTO != null ? orderDTO.getTempOrderId() : "unknown")
                .fulfillmentPlans(Collections.emptyList())
                .processingTimeMs(0L)
                .build();
    }
}
