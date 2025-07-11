package com.ordersourcing.engine.service;

import com.ordersourcing.engine.dto.OrderDTO;
import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.dto.SourcingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@TestPropertySource(properties = {
    "inventory.api.base-url=http://mock-inventory-api:8080/api/inventory",
    "inventory.api.timeout=5000"
})
public class BatchSourcingServiceTest {

    @Autowired
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
                        .build(),
                    OrderItemDTO.builder()
                        .sku("LAPTOP456")
                        .quantity(1)
                        .deliveryType("NEXT_DAY")
                        .locationFilterId("ELECTRONICS_SECURE_RULE")
                        .productCategory("ELECTRONICS")
                        .build(),
                    OrderItemDTO.builder()
                        .sku("HEADPHONES101")
                        .quantity(3)
                        .deliveryType("STANDARD")
                        .locationFilterId("STANDARD_DELIVERY_RULE")
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
    void testSourceOrderSimplifiedSingleItem() {
        // Test sourcing a single item order
        long startTime = System.currentTimeMillis();
        
        SourcingResponse response = batchSourcingService.sourceOrder(singleItemOrder);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Verify response structure
        assertNotNull(response, "Response should not be null");
        assertEquals("SINGLE_001", response.getOrderId(), "Order ID should match");
        assertNotNull(response.getFulfillmentPlans(), "Fulfillment plans should not be null");
        assertEquals(1, response.getFulfillmentPlans().size(), "Should have one fulfillment plan for single item");
        
        // Verify fulfillment plan
        SourcingResponse.FulfillmentPlan plan = response.getFulfillmentPlans().get(0);
        assertEquals("PHONE123", plan.getSku(), "SKU should match");
        assertEquals(1, plan.getRequestedQuantity(), "Requested quantity should be 1");
        assertNotNull(plan.getLocationAllocations(), "Location allocations should not be null");
        
        // Verify performance (should be well under 100ms for single item)
        assertTrue(executionTime < 100, 
            "Single item sourcing should complete in under 100ms. Actual: " + executionTime + "ms");
    }

    @Test
    void testSourceOrderSimplifiedMultiItem() {
        // Test sourcing a multi-item order
        long startTime = System.currentTimeMillis();
        
        SourcingResponse response = batchSourcingService.sourceOrder(multiItemOrder);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Verify response structure
        assertNotNull(response, "Response should not be null");
        assertEquals("MULTI_001", response.getOrderId(), "Order ID should match");
        assertNotNull(response.getFulfillmentPlans(), "Fulfillment plans should not be null");
        assertEquals(3, response.getFulfillmentPlans().size(), "Should have three fulfillment plans for multi-item order");
        
        // Verify each fulfillment plan has proper structure
        for (SourcingResponse.FulfillmentPlan plan : response.getFulfillmentPlans()) {
            assertNotNull(plan.getSku(), "SKU should not be null");
            assertTrue(plan.getRequestedQuantity() > 0, "Requested quantity should be positive");
            assertNotNull(plan.getLocationAllocations(), "Location allocations should not be null");
            
            // Verify location allocations have delivery timing
            for (SourcingResponse.LocationAllocation allocation : plan.getLocationAllocations()) {
                assertNotNull(allocation.getDeliveryTiming(), "Delivery timing should not be null");
                assertTrue(allocation.getAllocatedQuantity() > 0, "Allocated quantity should be positive");
            }
        }
        
        // Verify performance (should be under 200ms for multi-item)
        assertTrue(executionTime < 200, 
            "Multi-item sourcing should complete in under 200ms. Actual: " + executionTime + "ms");
    }

    @Test
    void testSourceOrderSimplifiedLargeOrder() {
        // Test sourcing a large order
        long startTime = System.currentTimeMillis();
        
        SourcingResponse response = batchSourcingService.sourceOrder(largeOrder);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Verify response structure
        assertNotNull(response, "Response should not be null");
        assertEquals("LARGE_001", response.getOrderId(), "Order ID should match");
        assertNotNull(response.getFulfillmentPlans(), "Fulfillment plans should not be null");
        assertEquals(15, response.getFulfillmentPlans().size(), "Should have 15 fulfillment plans for large order");
        
        // Verify helper methods work correctly
        int totalRequested = response.getTotalItemsRequested();
        int totalFulfilled = response.getTotalItemsFulfilled();
        assertTrue(totalRequested > 0, "Total requested should be positive");
        assertTrue(totalFulfilled >= 0, "Total fulfilled should be non-negative");
        
        // Verify performance (should be under 500ms for large orders)
        assertTrue(executionTime < 500, 
            "Large order sourcing should complete in under 500ms. Actual: " + executionTime + "ms");
    }

    @Test
    void testResponseHelperMethods() {
        // Test helper methods on SourcingResponse
        SourcingResponse response = batchSourcingService.sourceOrder(multiItemOrder);
        
        assertNotNull(response, "Response should not be null");
        
        // Test helper methods
        int totalRequested = response.getTotalItemsRequested();
        int totalFulfilled = response.getTotalItemsFulfilled();
        boolean hasPartial = response.hasPartialFulfillments();
        
        assertTrue(totalRequested > 0, "Total requested should be positive");
        assertTrue(totalFulfilled >= 0, "Total fulfilled should be non-negative");
        // hasPartial can be true or false, both are valid
        
        // Verify processing time is recorded
        assertTrue(response.getProcessingTimeMs() >= 0, "Processing time should be non-negative");
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
                .requestTimestamp(LocalDateTime.now())
                .orderItems(Collections.emptyList())
                .build();
        assertFalse(isValidOrder(emptyOrder), "Empty order should be invalid");
        
        // Invalid order - missing location filter
        OrderDTO invalidOrder = OrderDTO.builder()
                .tempOrderId("INVALID_001")
                .latitude(40.7128)
                .longitude(-74.0060)
                .requestTimestamp(LocalDateTime.now())
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
    void testFulfillmentPlanStructure() {
        // Test that fulfillment plans have correct structure
        SourcingResponse response = batchSourcingService.sourceOrder(multiItemOrder);
        
        assertNotNull(response.getFulfillmentPlans(), "Fulfillment plans should not be null");
        assertFalse(response.getFulfillmentPlans().isEmpty(), "Should have fulfillment plans");
        
        for (SourcingResponse.FulfillmentPlan plan : response.getFulfillmentPlans()) {
            // Verify basic plan structure
            assertNotNull(plan.getSku(), "SKU should not be null");
            assertTrue(plan.getRequestedQuantity() > 0, "Requested quantity should be positive");
            assertTrue(plan.getTotalFulfilled() >= 0, "Total fulfilled should be non-negative");
            assertTrue(plan.getOverallScore() >= 0, "Overall score should be non-negative");
            
            // Verify location allocations
            assertNotNull(plan.getLocationAllocations(), "Location allocations should not be null");
            for (SourcingResponse.LocationAllocation allocation : plan.getLocationAllocations()) {
                assertTrue(allocation.getLocationId() > 0, "Location ID should be positive");
                assertNotNull(allocation.getLocationName(), "Location name should not be null");
                assertTrue(allocation.getAllocatedQuantity() > 0, "Allocated quantity should be positive");
                assertTrue(allocation.getLocationScore() >= 0, "Location score should be non-negative");
                
                // Verify delivery timing
                SourcingResponse.DeliveryTiming timing = allocation.getDeliveryTiming();
                assertNotNull(timing, "Delivery timing should not be null");
                assertNotNull(timing.getEstimatedShipDate(), "Ship date should not be null");
                assertNotNull(timing.getEstimatedDeliveryDate(), "Delivery date should not be null");
                assertTrue(timing.getTransitTimeDays() >= 0, "Transit time should be non-negative");
                assertTrue(timing.getProcessingTimeHours() >= 0, "Processing time should be non-negative");
            }
        }
    }

    @Test
    void testElectronicsOrderSourcing() {
        // Test sourcing of electronics orders
        OrderDTO electronicsOrder = OrderDTO.builder()
                .tempOrderId("ELECTRONICS_001")
                .latitude(40.7128)
                .longitude(-74.0060)
                .requestTimestamp(LocalDateTime.now())
                .orderItems(Arrays.asList(
                    OrderItemDTO.builder()
                        .sku("LAPTOP456")
                        .quantity(1)
                        .deliveryType("NEXT_DAY")
                        .locationFilterId("ELECTRONICS_SECURE_RULE")
                        .productCategory("ELECTRONICS_COMPUTER")
                        .build()
                ))
                .build();
        
        SourcingResponse response = batchSourcingService.sourceOrder(electronicsOrder);
        
        assertNotNull(response, "Response should not be null");
        assertEquals("ELECTRONICS_001", response.getOrderId(), "Order ID should match");
        assertEquals(1, response.getFulfillmentPlans().size(), "Should have one fulfillment plan");
        
        SourcingResponse.FulfillmentPlan plan = response.getFulfillmentPlans().get(0);
        assertEquals("LAPTOP456", plan.getSku(), "SKU should match");
        assertEquals(1, plan.getRequestedQuantity(), "Requested quantity should be 1");
        
        // Verify that the order input uses security filter
        OrderItemDTO item = electronicsOrder.getOrderItems().get(0);
        assertEquals("ELECTRONICS_SECURE_RULE", item.getLocationFilterId(), 
            "Electronics items should use security filter");
    }

    @Test
    void testSameDayDeliverySourcing() {
        // Test sourcing of same day delivery orders
        OrderDTO sameDayOrder = OrderDTO.builder()
                .tempOrderId("SAME_DAY_001")
                .latitude(40.7128)
                .longitude(-74.0060)
                .requestTimestamp(LocalDateTime.now())
                .orderItems(Arrays.asList(
                    OrderItemDTO.builder()
                        .sku("PHONE123")
                        .quantity(1)
                        .deliveryType("SAME_DAY")
                        .locationFilterId("SDD_FILTER_RULE")
                        .build()
                ))
                .build();
        
        SourcingResponse response = batchSourcingService.sourceOrder(sameDayOrder);
        
        assertNotNull(response, "Response should not be null");
        assertEquals("SAME_DAY_001", response.getOrderId(), "Order ID should match");
        assertEquals(1, response.getFulfillmentPlans().size(), "Should have one fulfillment plan");
        
        SourcingResponse.FulfillmentPlan plan = response.getFulfillmentPlans().get(0);
        assertEquals("PHONE123", plan.getSku(), "SKU should match");
        
        // Verify delivery timing is available
        for (SourcingResponse.LocationAllocation allocation : plan.getLocationAllocations()) {
            SourcingResponse.DeliveryTiming timing = allocation.getDeliveryTiming();
            assertNotNull(timing, "Delivery timing should not be null for same day delivery");
            // Same day delivery should have short transit time
            assertTrue(timing.getTransitTimeDays() <= 1, "Same day delivery should have transit time of 1 day or less");
        }
    }

    @Test
    void testErrorHandling() {
        // Test error handling scenarios
        
        // Null order
        assertThrows(Exception.class, () -> {
            batchSourcingService.sourceOrder(null);
        }, "Null order should throw exception");
        
        // Order with invalid coordinates
        OrderDTO invalidCoordsOrder = OrderDTO.builder()
                .tempOrderId("INVALID_COORDS")
                .latitude(200.0) // Invalid latitude
                .longitude(-74.0060)
                .requestTimestamp(LocalDateTime.now())
                .orderItems(Arrays.asList(
                    OrderItemDTO.builder()
                        .sku("TEST123")
                        .quantity(1)
                        .deliveryType("STANDARD")
                        .locationFilterId("STANDARD_DELIVERY_RULE")
                        .build()
                ))
                .build();
        
        assertThrows(Exception.class, () -> {
            batchSourcingService.sourceOrder(invalidCoordsOrder);
        }, "Invalid coordinates should throw exception");
    }

    // Helper methods for testing
    
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
                    .build());
        }
        
        return items;
    }
}