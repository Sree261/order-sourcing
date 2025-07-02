package com.ordersourcing.engine.controller;

import com.ordersourcing.engine.model.FulfillmentPlan;
import com.ordersourcing.engine.model.Order;
import com.ordersourcing.engine.model.OrderItem;
import com.ordersourcing.engine.repository.OrderRepository;
import com.ordersourcing.engine.service.PromiseDateService;
import com.ordersourcing.engine.service.BatchSourcingService;
import com.ordersourcing.engine.service.InventoryApiService;
import com.ordersourcing.engine.service.LocationFilterExecutionService;
import com.ordersourcing.engine.repository.LocationFilterRepository;
import com.ordersourcing.engine.dto.OrderDTO;
import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.dto.SourcingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sourcing")
@Slf4j
public class SourcingController {


    @Autowired
    private PromiseDateService promiseDateService;

    @Autowired
    private BatchSourcingService batchSourcingService;

    @Autowired
    private InventoryApiService inventoryApiService;

    @Autowired
    private LocationFilterExecutionService locationFilterService;

    @Autowired
    private LocationFilterRepository locationFilterRepository;

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping("/source")
    public ResponseEntity<?> sourceOrder(@RequestParam Integer orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

            // Convert Order to OrderDTO for the new API
            OrderDTO orderDTO = convertToOrderDTO(order);
            SourcingResponse response = batchSourcingService.sourceOrder(orderDTO);

            if (response.getFulfillmentPlans().isEmpty()) {
                return ResponseEntity.ok()
                    .body(Map.of("message", "No fulfillment plans could be generated for the order"));
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An error occurred while processing the request: " + e.getMessage()));
        }
    }

    @PostMapping("/source-with-promise-dates")
    public ResponseEntity<?> sourceOrderWithPromiseDates(@RequestParam Integer orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

            // Convert Order to OrderDTO and use the new optimized API
            OrderDTO orderDTO = convertToOrderDTO(order);
            SourcingResponse response = batchSourcingService.sourceOrder(orderDTO);

            if (response.getFulfillmentPlans().isEmpty()) {
                return ResponseEntity.ok()
                    .body(Map.of("message", "No fulfillment plans could be generated for the order"));
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An error occurred while processing the request: " + e.getMessage()));
        }
    }

    @PostMapping("/source-realtime")
    public ResponseEntity<SourcingResponse> sourceRealtimeOrder(@RequestBody @Valid OrderDTO orderDTO) {
        try {
            log.info("Received real-time sourcing request for order: {} with {} items", 
                    orderDTO.getTempOrderId(), orderDTO.getOrderItems().size());
            
            // Validate order items have location filter IDs
            for (OrderItemDTO item : orderDTO.getOrderItems()) {
                if (item.getLocationFilterId() == null || item.getLocationFilterId().trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(createErrorSourcingResponse(orderDTO, 
                                    "Missing location filter ID for item: " + item.getSku()));
                }
            }
            
            // Execute optimized batch sourcing
            SourcingResponse response = batchSourcingService.sourceOrder(orderDTO);
            
            // Add success indicators
            if (response.getFulfillmentPlans().isEmpty()) {
                log.warn("No fulfillment plans generated for order: {}", orderDTO.getTempOrderId());
                response.getMetadata().getPerformanceWarnings().add("No fulfillment plans could be generated");
            }
            
            log.info("Completed real-time sourcing for order: {} in {}ms", 
                    orderDTO.getTempOrderId(), response.getMetadata().getTotalProcessingTimeMs());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for real-time sourcing: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorSourcingResponse(orderDTO, "Invalid request: " + e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error in real-time sourcing for order: {}", 
                    orderDTO != null ? orderDTO.getTempOrderId() : "unknown", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorSourcingResponse(orderDTO, "Internal error: " + e.getMessage()));
        }
    }

    @GetMapping("/source-realtime/health")
    public ResponseEntity<Map<String, Object>> getRealtimeSourcingHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("service", "real-time-sourcing");
            
            // Add inventory service status
            health.put("inventoryService", Map.of(
                    "type", "database",
                    "status", "UP"
            ));
            
            // Add filter metrics
            var filterMetrics = locationFilterService.getFilterMetrics();
            health.put("filterMetrics", Map.of(
                    "totalFilters", filterMetrics.size(),
                    "averageCacheHitRate", filterMetrics.values().stream()
                            .mapToDouble(LocationFilterExecutionService.FilterMetrics::getCacheHitRate)
                            .average().orElse(0.0)
            ));
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("Error getting health status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "DOWN", "error", e.getMessage()));
        }
    }

    @PostMapping("/source-realtime/validate")
    public ResponseEntity<Map<String, Object>> validateRealtimeOrder(@RequestBody @Valid OrderDTO orderDTO) {
        try {
            Map<String, Object> validation = new HashMap<>();
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            
            // Validate order structure
            if (orderDTO.getOrderItems().isEmpty()) {
                errors.add("Order must contain at least one item");
            }
            
            // Validate location filters
            Set<String> filterIds = orderDTO.getOrderItems().stream()
                    .map(OrderItemDTO::getLocationFilterId)
                    .collect(Collectors.toSet());
            
            for (String filterId : filterIds) {
                if (!locationFilterRepository.findByIdAndIsActiveTrue(filterId).isPresent()) {
                    errors.add("Invalid or inactive location filter: " + filterId);
                }
            }
            
            // Performance warnings
            if (orderDTO.getOrderItems().size() > 20) {
                warnings.add("Large order may take longer to process");
            }
            
            if (orderDTO.hasHighSecurityItems()) {
                warnings.add("High security items may require additional processing time");
            }
            
            validation.put("isValid", errors.isEmpty());
            validation.put("errors", errors);
            validation.put("warnings", warnings);
            validation.put("estimatedProcessingStrategy", 
                    orderDTO.getOrderItems().size() >= 3 ? "BATCH" : "SEQUENTIAL");
            
            return ResponseEntity.ok(validation);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("isValid", false, "errors", 
                            Arrays.asList("Validation error: " + e.getMessage())));
        }
    }

    private SourcingResponse createErrorSourcingResponse(OrderDTO orderDTO, String errorMessage) {
        return SourcingResponse.builder()
                .tempOrderId(orderDTO != null ? orderDTO.getTempOrderId() : "unknown")
                .fulfillmentPlans(Collections.emptyList())
                .metadata(SourcingResponse.SourcingMetadata.builder()
                        .totalProcessingTimeMs(0L)
                        .performanceWarnings(Arrays.asList(errorMessage))
                        .requestTimestamp(orderDTO != null ? orderDTO.getRequestTimestamp() : LocalDateTime.now())
                        .responseTimestamp(LocalDateTime.now())
                        .build())
                .build();
    }

    private OrderDTO convertToOrderDTO(Order order) {
        List<OrderItemDTO> orderItemDTOs = new ArrayList<>();
        
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                orderItemDTOs.add(OrderItemDTO.builder()
                    .sku(item.getSku())
                    .quantity(item.getQuantity())
                    .deliveryType(item.getDeliveryType())
                    .locationFilterId("STANDARD_DELIVERY_RULE") // Default filter
                    .build());
            }
        }
        
        return OrderDTO.builder()
            .tempOrderId(order.getOrderId())
            .latitude(order.getLatitude())
            .longitude(order.getLongitude())
            .orderItems(orderItemDTOs)
            .requestTimestamp(LocalDateTime.now())
            .build();
    }
}
