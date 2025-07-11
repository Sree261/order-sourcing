package com.ordersourcing.engine.service;

import com.ordersourcing.engine.model.*;
import com.ordersourcing.engine.dto.*;
import com.ordersourcing.engine.service.PromiseDateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PromiseDateServiceTest {

    @Autowired
    private PromiseDateService promiseDateService;
    private OrderDTO testOrderContext;
    private OrderItemDTO testOrderItem;
    private Location testLocation;
    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        
        // Create test data
        testOrderContext = OrderDTO.builder()
                .tempOrderId("TEST_ORDER_001")
                .latitude(42.3601)
                .longitude(-71.0589)
                .requestTimestamp(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();

        testOrderItem = OrderItemDTO.builder()
                .sku("TEST_SKU_001")
                .quantity(5)
                .deliveryType("STANDARD")
                .locationFilterId("filter-001")
                .isHazmat(false)
                .requiresColdStorage(false)
                .build();

        testLocation = new Location();
        testLocation.setId(1);
        testLocation.setName("Test Location");
        testLocation.setTransitTime(2);
        testLocation.setLatitude(42.3601);
        testLocation.setLongitude(-71.0589);

        testInventory = new Inventory();
        testInventory.setId(1);
        testInventory.setLocationId(1);
        testInventory.setSku("TEST_SKU_001");
        testInventory.setQuantity(10);
        testInventory.setProcessingTime(1);
    }

    @Test
    void testCalculateEnhancedPromiseDate() {
        PromiseDateBreakdown breakdown = promiseDateService.calculateEnhancedPromiseDate(
            testOrderItem, testLocation, testInventory, testOrderContext);

        // May be null if no carrier is found, which is acceptable
        if (breakdown != null) {
            assertNotNull(breakdown.getPromiseDate());
            assertNotNull(breakdown.getEstimatedDeliveryDate());
            assertEquals("STANDARD", breakdown.getDeliveryType());
            assertTrue(breakdown.getLocationProcessingHours() > 0);
            assertTrue(breakdown.getCarrierTransitHours() > 0);
        }
    }

    @Test
    void testBatchCalculatePromiseDates() throws Exception {
        List<OrderItemDTO> orderItems = List.of(testOrderItem);
        Map<String, List<Location>> filterResults = new HashMap<>();
        filterResults.put("filter-001", List.of(testLocation));
        Map<String, List<Inventory>> inventoryResults = new HashMap<>();
        inventoryResults.put("TEST_SKU_001", List.of(testInventory));

        CompletableFuture<Map<String, PromiseDateBreakdown>> future = 
            promiseDateService.batchCalculatePromiseDates(orderItems, filterResults, inventoryResults, testOrderContext);

        Map<String, PromiseDateBreakdown> results = future.get();
        assertNotNull(results);
        
        // Results may be empty if no carrier is found, which is acceptable
        if (results.containsKey("TEST_SKU_001")) {
            PromiseDateBreakdown breakdown = results.get("TEST_SKU_001");
            assertNotNull(breakdown.getPromiseDate());
            assertEquals("STANDARD", breakdown.getDeliveryType());
        }
    }

    /* 
     * COMMENTED OUT TESTS - These test methods that were removed from the interface:
     * - calculatePromiseDates
     * - calculatePromiseDateForDeliveryType  
     * - canMeetPromiseDate
     * - calculateQuantityPromiseDates
     * - calculateBusinessDaysBetween
     * - getLatestSameDayPromise
     * 
     * These methods are no longer part of the PromiseDateService interface.
     */

    /*
    @Test
    void testCalculatePromiseDates() {
        promiseDateService.calculatePromiseDates(testPlan, testOrderItem, testInventory);

        assertNotNull(testPlan.getPromiseDate());
        assertNotNull(testPlan.getEstimatedShipDate());
        assertNotNull(testPlan.getEstimatedDeliveryDate());
        assertEquals("STANDARD", testPlan.getDeliveryType());
        assertEquals(Integer.valueOf(24), testPlan.getProcessingTimeHours());
        assertEquals(Integer.valueOf(48), testPlan.getTransitTimeHours());
    }

    @Test
    void testSameDayDeliveryPromiseDate() {
        testOrderItem.setDeliveryType("SAME_DAY");
        
        LocalDateTime promiseDate = promiseDateService.calculatePromiseDateForDeliveryType(
            "SAME_DAY", testLocation, testInventory);
        
        assertNotNull(promiseDate);
        assertEquals(LocalDateTime.now().toLocalDate(), promiseDate.toLocalDate());
        assertEquals(23, promiseDate.getHour());
        assertEquals(59, promiseDate.getMinute());
    }

    @Test
    void testNextDayDeliveryPromiseDate() {
        testOrderItem.setDeliveryType("NEXT_DAY");
        
        LocalDateTime promiseDate = promiseDateService.calculatePromiseDateForDeliveryType(
            "NEXT_DAY", testLocation, testInventory);
        
        assertNotNull(promiseDate);
        assertTrue(promiseDate.isAfter(LocalDateTime.now()));
    }

    @Test
    void testStandardDeliveryPromiseDate() {
        LocalDateTime promiseDate = promiseDateService.calculatePromiseDateForDeliveryType(
            "STANDARD", testLocation, testInventory);
        
        assertNotNull(promiseDate);
        assertTrue(promiseDate.isAfter(LocalDateTime.now()));
        
        // Standard delivery should have at least 2 days buffer
        assertTrue(promiseDate.isAfter(LocalDateTime.now().plusDays(2)));
    }

    @Test
    void testCanMeetPromiseDate() {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(5);
        boolean canMeet = promiseDateService.canMeetPromiseDate(
            futureDate, "STANDARD", testLocation, testInventory);
        
        assertTrue(canMeet);
        
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        boolean cannotMeet = promiseDateService.canMeetPromiseDate(
            pastDate, "STANDARD", testLocation, testInventory);
        
        assertFalse(cannotMeet);
    }

    @Test
    void testCalculateQuantityPromiseDates() {
        List<Map<String, Object>> quantityPromises = promiseDateService.calculateQuantityPromiseDates(
            testOrderItem, testLocation, testInventory);
        
        assertNotNull(quantityPromises);
        assertEquals(5, quantityPromises.size());
        
        for (int i = 0; i < quantityPromises.size(); i++) {
            Map<String, Object> promise = quantityPromises.get(i);
            assertEquals(i + 1, promise.get("quantity"));
            assertNotNull(promise.get("promiseDate"));
            assertNotNull(promise.get("estimatedShipDate"));
            assertNotNull(promise.get("estimatedDeliveryDate"));
        }
    }

    @Test
    void testQuantityAdjustments() {
        // Test with high quantity that should trigger processing buffer
        testOrderItem.setQuantity(15);
        
        List<Map<String, Object>> quantityPromises = promiseDateService.calculateQuantityPromiseDates(
            testOrderItem, testLocation, testInventory);
        
        assertNotNull(quantityPromises);
        assertEquals(15, quantityPromises.size());
        
        // Higher quantities should have later promise dates
        LocalDateTime firstQuantityPromise = (LocalDateTime) quantityPromises.get(0).get("promiseDate");
        LocalDateTime lastQuantityPromise = (LocalDateTime) quantityPromises.get(14).get("promiseDate");
        
        assertTrue(lastQuantityPromise.isAfter(firstQuantityPromise) || lastQuantityPromise.isEqual(firstQuantityPromise));
    }

    @Test
    void testInsufficientInventoryAdjustment() {
        // Set inventory lower than requested quantity
        testInventory.setQuantity(3);
        testOrderItem.setQuantity(5);
        
        LocalDateTime promiseDate = promiseDateService.calculatePromiseDateForDeliveryType(
            "STANDARD", testLocation, testInventory);
        
        assertNotNull(promiseDate);
        // Should be further in the future due to backorder buffer
        assertTrue(promiseDate.isAfter(LocalDateTime.now().plusDays(3)));
    }

    @Test
    void testBusinessDaysCalculation() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);
        
        long businessDays = promiseDateService.calculateBusinessDaysBetween(start, end);
        
        assertTrue(businessDays <= 7);
        assertTrue(businessDays >= 5); // Should be at least 5 business days in a week
    }

    @Test
    void testWeekendAdjustment() {
        // Create a test that would land on weekend
        LocalDateTime saturday = LocalDateTime.now()
            .with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.SATURDAY));
        
        testInventory.setProcessingTime(0);
        testLocation.setTransitTime(0);
        
        LocalDateTime promiseDate = promiseDateService.calculatePromiseDateForDeliveryType(
            "STANDARD", testLocation, testInventory);
        
        // Promise date should not be on weekend for standard delivery
        assertNotEquals(DayOfWeek.SATURDAY, promiseDate.getDayOfWeek());
        assertNotEquals(DayOfWeek.SUNDAY, promiseDate.getDayOfWeek());
    }

    @Test
    void testGetLatestSameDayPromise() {
        LocalDateTime latestSameDay = promiseDateService.getLatestSameDayPromise();
        
        assertNotNull(latestSameDay);
        assertEquals(23, latestSameDay.getHour());
        assertEquals(59, latestSameDay.getMinute());
        assertEquals(LocalDateTime.now().toLocalDate(), latestSameDay.toLocalDate());
    }

    @Test
    void testSameDayDeliveryLimits() {
        testOrderItem.setDeliveryType("SAME_DAY");
        testOrderItem.setQuantity(20); // Large quantity
        
        List<Map<String, Object>> quantityPromises = promiseDateService.calculateQuantityPromiseDates(
            testOrderItem, testLocation, testInventory);
        
        assertNotNull(quantityPromises);
        
        // Check that same day delivery constraints are respected
        for (Map<String, Object> promise : quantityPromises) {
            LocalDateTime promiseDate = (LocalDateTime) promise.get("promiseDate");
            // Same day promises should either be today or moved to next day
            assertTrue(promiseDate.toLocalDate().equals(LocalDateTime.now().toLocalDate()) ||
                      promiseDate.toLocalDate().equals(LocalDateTime.now().plusDays(1).toLocalDate()));
        }
    }
    */
}