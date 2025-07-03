package com.ordersourcing.engine.service;

import com.ordersourcing.engine.model.*;
import com.ordersourcing.engine.service.PromiseDateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PromiseDateServiceTest {

    @Autowired
    private PromiseDateService promiseDateService;
    private Order testOrder;
    private OrderItem testOrderItem;
    private Location testLocation;
    private Inventory testInventory;
    private FulfillmentPlan testPlan;

    @BeforeEach
    void setUp() {
        
        // Create test data
        testOrder = new Order();
        testOrder.setId(1);
        testOrder.setOrderId("TEST_ORDER_001");
        testOrder.setLatitude(42.3601);
        testOrder.setLongitude(-71.0589);

        testOrderItem = new OrderItem();
        testOrderItem.setId(1);
        testOrderItem.setSku("TEST_SKU_001");
        testOrderItem.setQuantity(5);
        testOrderItem.setDeliveryType("STANDARD");

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

        testPlan = new FulfillmentPlan();
        testPlan.setOrder(testOrder);
        testPlan.setLocation(testLocation);
        testPlan.setSku("TEST_SKU_001");
        testPlan.setQuantity(5);
    }

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
}