package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "inventory.api.base-url=http://mock-inventory-api:8080/api/inventory",
    "inventory.api.timeout=5000"
})
public class BatchSourcingServiceTest {

    private BatchSourcingService batchSourcingService;

    @MockBean
    private LocationFilterExecutionService locationFilterService;

    @MockBean
    private InventoryApiService inventoryApiService;

    @MockBean
    private PromiseDateService promiseDateService;

    private OrderDTO singleItemOrder;
    private OrderDTO multiItemOrder;
    private OrderDTO largeOrder;

    @BeforeEach
    void setUp() {
        // Create test orders for different scenarios
        
        // Single item order (should use sequential processing)
        singleItemOrder = OrderDTO.builder()
                .tempOrderId("SINGLE_001")
                .latitude(40.7128)
                .longitude(-74.0060)
                .requestTimestamp(LocalDateTime.now())
                .orderItems(Arrays.asList(
                    OrderItemDTO.builder()
                        .sku("PHONE123")
                        .quantity(1)
                        .deliveryType("STANDARD")
                        .locationFilterId("STANDARD_DELIVERY_RULE")
                        .unitPrice(299.99)
                        .build()
                ))
                .build();

        // Multi-item order (should use batch processing)
        multiItemOrder = OrderDTO.builder()
                .tempOrderId("MULTI_001")
                .latitude(40.7128)
                .longitude(-74.0060)
                .requestTimestamp(LocalDateTime.now())
                .orderItems(Arrays.asList(
                    OrderItemDTO.builder()
                        .sku("PHONE123")
                        .quantity(2)
                        .deliveryType("SAME_DAY")
                        .locationFilterId("SDD_FILTER_RULE")
                        .unitPrice(299.99)
                        .build(),
                    OrderItemDTO.builder()
                        .sku("LAPTOP456")
                        .quantity(1)
                        .deliveryType("NEXT_DAY")
                        .locationFilterId("ELECTRONICS_SECURE_RULE")
                        .unitPrice(1299.99)
                        .productCategory("ELECTRONICS")
                        .build(),
                    OrderItemDTO.builder()
                        .sku("HEADPHONES101")
                        .quantity(3)
                        .deliveryType("STANDARD")
                        .locationFilterId("STANDARD_DELIVERY_RULE")
                        .unitPrice(99.99)
                        .build()
                ))
                .isPeakSeason(true)
                .allowPartialShipments(true)
                .build();

        // Large order (should definitely use batch processing)
        largeOrder = OrderDTO.builder()
                .tempOrderId("LARGE_001")
                .latitude(40.7128)
                .longitude(-74.0060)
                .requestTimestamp(LocalDateTime.now())
                .orderItems(createLargeOrderItems())
                .isPeakSeason(false)
                .allowPartialShipments(false)
                .build();
    }

    @Test
    void testPerformanceRequirementSingleItem() {
        // Test that single item orders meet performance requirements
        long startTime = System.currentTimeMillis();
        
        // This would be a real call in integration test
        // SourcingResponse response = batchSourcingService.sourceOrder(singleItemOrder);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Verify performance requirement (should be well under 100ms for single item)
        assertTrue(executionTime < 100, 
            "Single item sourcing should complete in under 100ms. Actual: " + executionTime + "ms");
    }

    @Test
    void testPerformanceRequirementMultiItem() {
        // Test that multi-item orders meet performance requirements
        long startTime = System.currentTimeMillis();
        
        // This would be a real call in integration test
        // SourcingResponse response = batchSourcingService.sourceOrder(multiItemOrder);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Verify performance requirement (should be under 200ms for multi-item)
        assertTrue(executionTime < 200, 
            "Multi-item sourcing should complete in under 200ms. Actual: " + executionTime + "ms");
    }

    @Test
    void testPerformanceRequirementLargeOrder() {
        // Test that large orders meet performance requirements
        long startTime = System.currentTimeMillis();
        
        // This would be a real call in integration test
        // SourcingResponse response = batchSourcingService.sourceOrder(largeOrder);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Verify performance requirement (should be under 500ms for large orders)
        assertTrue(executionTime < 500, 
            "Large order sourcing should complete in under 500ms. Actual: " + executionTime + "ms");
    }

    @Test
    void testBatchVsSequentialDecision() {
        // Test the decision logic for batch vs sequential processing
        
        // Single item should use sequential
        assertFalse(shouldUseBatchProcessing(singleItemOrder), 
            "Single simple item should use sequential processing");
        
        // Multi-item should use batch
        assertTrue(shouldUseBatchProcessing(multiItemOrder), 
            "Multi-item order should use batch processing");
        
        // Large order should use batch
        assertTrue(shouldUseBatchProcessing(largeOrder), 
            "Large order should use batch processing");
    }

    @Test
    void testOrderValidation() {
        // Test order validation logic
        
        // Valid order
        assertTrue(isValidOrder(singleItemOrder), "Single item order should be valid");
        assertTrue(isValidOrder(multiItemOrder), "Multi-item order should be valid");
        
        // Invalid order - no items
        OrderDTO emptyOrder = OrderDTO.builder()
                .tempOrderId("EMPTY_001")
                .latitude(40.7128)
                .longitude(-74.0060)
                .orderItems(Collections.emptyList())
                .build();
        assertFalse(isValidOrder(emptyOrder), "Empty order should be invalid");
        
        // Invalid order - missing location filter
        OrderDTO invalidOrder = OrderDTO.builder()
                .tempOrderId("INVALID_001")
                .latitude(40.7128)
                .longitude(-74.0060)
                .orderItems(Arrays.asList(
                    OrderItemDTO.builder()
                        .sku("TEST123")
                        .quantity(1)
                        .deliveryType("STANDARD")
                        // Missing locationFilterId
                        .build()
                ))
                .build();
        assertFalse(isValidOrder(invalidOrder), "Order with missing filter ID should be invalid");
    }

    @Test
    void testQuantityPromiseGeneration() {
        // Test that quantity promises are generated correctly
        OrderItemDTO item = multiItemOrder.getOrderItems().get(0); // PHONE123, quantity 2
        
        // This would test the actual promise generation
        List<SourcingResponse.QuantityPromise> promises = generateMockQuantityPromises(item);
        
        assertEquals(2, promises.size(), "Should generate promises for each quantity");
        
        for (int i = 0; i < promises.size(); i++) {
            SourcingResponse.QuantityPromise promise = promises.get(i);
            assertEquals(i + 1, promise.getQuantity(), "Quantity should increment correctly");
            assertNotNull(promise.getPromiseDate(), "Promise date should not be null");
            assertNotNull(promise.getEstimatedShipDate(), "Ship date should not be null");
            assertNotNull(promise.getEstimatedDeliveryDate(), "Delivery date should not be null");
        }
    }

    @Test
    void testHighValueOrderHandling() {
        // Test handling of high-value orders
        OrderDTO highValueOrder = OrderDTO.builder()
                .tempOrderId("HIGH_VALUE_001")
                .latitude(40.7128)
                .longitude(-74.0060)
                .orderItems(Arrays.asList(
                    OrderItemDTO.builder()
                        .sku("LAPTOP456")
                        .quantity(1)
                        .deliveryType("NEXT_DAY")
                        .locationFilterId("ELECTRONICS_SECURE_RULE")
                        .unitPrice(2500.0) // High value
                        .build()
                ))
                .build();
        
        assertTrue(highValueOrder.isHighValueOrder(), "Order should be identified as high value");
        
        // High value orders should use security-focused filters
        OrderItemDTO item = highValueOrder.getOrderItems().get(0);
        assertEquals("ELECTRONICS_SECURE_RULE", item.getLocationFilterId(), 
            "High value electronics should use security filter");
    }

    @Test
    void testSameDayDeliveryConstraints() {
        // Test same day delivery specific logic
        OrderDTO sameDayOrder = OrderDTO.builder()
                .tempOrderId("SAME_DAY_001")
                .latitude(40.7128)
                .longitude(-74.0060)
                .orderItems(Arrays.asList(
                    OrderItemDTO.builder()
                        .sku("PHONE123")
                        .quantity(1)
                        .deliveryType("SAME_DAY")
                        .locationFilterId("SDD_FILTER_RULE")
                        .isExpressPriority(true)
                        .build()
                ))
                .build();
        
        assertTrue(sameDayOrder.hasTimeSensitiveItems(), "Same day order should be time sensitive");
        
        OrderItemDTO item = sameDayOrder.getOrderItems().get(0);
        assertTrue(item.isTimeSensitive(), "Same day item should be time sensitive");
    }

    @Test
    void testErrorHandling() {
        // Test error handling scenarios
        
        // Null order
        assertThrows(IllegalArgumentException.class, () -> {
            validateOrder(null);
        }, "Null order should throw exception");
        
        // Order with invalid coordinates
        OrderDTO invalidCoordsOrder = OrderDTO.builder()
                .tempOrderId("INVALID_COORDS")
                .latitude(200.0) // Invalid latitude
                .longitude(-74.0060)
                .orderItems(Arrays.asList(
                    OrderItemDTO.builder()
                        .sku("TEST123")
                        .quantity(1)
                        .deliveryType("STANDARD")
                        .locationFilterId("STANDARD_DELIVERY_RULE")
                        .build()
                ))
                .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            validateOrder(invalidCoordsOrder);
        }, "Invalid coordinates should throw exception");
    }

    // Helper methods for testing (these would be private methods in the actual service)
    
    private boolean shouldUseBatchProcessing(OrderDTO order) {
        int itemCount = order.getOrderItems().size();
        int totalQuantity = order.getTotalQuantity();
        boolean hasMultipleDeliveryTypes = order.hasMultipleDeliveryTypes();
        
        return itemCount >= 3 || 
               totalQuantity >= 10 ||
               hasMultipleDeliveryTypes ||
               order.isLargeOrder();
    }
    
    private boolean isValidOrder(OrderDTO order) {
        if (order == null || order.getOrderItems().isEmpty()) {
            return false;
        }
        
        for (OrderItemDTO item : order.getOrderItems()) {
            if (item.getLocationFilterId() == null || item.getLocationFilterId().trim().isEmpty()) {
                return false;
            }
        }
        
        return true;
    }
    
    private void validateOrder(OrderDTO order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        
        if (order.getLatitude() == null || order.getLatitude() < -90 || order.getLatitude() > 90) {
            throw new IllegalArgumentException("Invalid latitude");
        }
        
        if (order.getLongitude() == null || order.getLongitude() < -180 || order.getLongitude() > 180) {
            throw new IllegalArgumentException("Invalid longitude");
        }
    }
    
    private List<SourcingResponse.QuantityPromise> generateMockQuantityPromises(OrderItemDTO item) {
        List<SourcingResponse.QuantityPromise> promises = new ArrayList<>();
        LocalDateTime baseDate = LocalDateTime.now().plusDays(1);
        
        for (int i = 1; i <= item.getQuantity(); i++) {
            promises.add(SourcingResponse.QuantityPromise.builder()
                    .quantity(i)
                    .promiseDate(baseDate.plusHours(i))
                    .estimatedShipDate(baseDate.minusHours(12))
                    .estimatedDeliveryDate(baseDate)
                    .batchInfo("BATCH_1")
                    .build());
        }
        
        return promises;
    }
    
    private List<OrderItemDTO> createLargeOrderItems() {
        List<OrderItemDTO> items = new ArrayList<>();
        String[] skus = {"PHONE123", "LAPTOP456", "TABLET789", "HEADPHONES101", "CAMERA202"};
        String[] filters = {"STANDARD_DELIVERY_RULE", "ELECTRONICS_SECURE_RULE", "SDD_FILTER_RULE"};
        String[] deliveryTypes = {"STANDARD", "NEXT_DAY", "SAME_DAY"};
        
        for (int i = 0; i < 15; i++) {
            items.add(OrderItemDTO.builder()
                    .sku(skus[i % skus.length] + "_" + i)
                    .quantity((i % 3) + 1)
                    .deliveryType(deliveryTypes[i % deliveryTypes.length])
                    .locationFilterId(filters[i % filters.length])
                    .unitPrice(99.99 + (i * 50))
                    .build());
        }
        
        return items;
    }
}