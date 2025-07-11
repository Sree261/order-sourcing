package com.ordersourcing.engine.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourcingResponse {
    
    private String orderId;
    private List<FulfillmentPlan> fulfillmentPlans;
    private long processingTimeMs;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FulfillmentPlan {
        private String sku;
        private int requestedQuantity;
        private int totalFulfilled;
        private boolean isPartialFulfillment;
        private double overallScore;
        private List<LocationAllocation> locationAllocations;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationAllocation {
        private int locationId;
        private String locationName;
        private int allocatedQuantity;
        private double locationScore;
        private DeliveryTiming deliveryTiming;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeliveryTiming {
        private LocalDateTime estimatedShipDate;
        private LocalDateTime estimatedDeliveryDate;
        private int transitTimeDays;
        private int processingTimeHours;
    }
    
    // Helper methods
    public boolean hasPartialFulfillments() {
        return fulfillmentPlans.stream()
                .anyMatch(FulfillmentPlan::isPartialFulfillment);
    }
    
    public int getTotalItemsRequested() {
        return fulfillmentPlans.stream()
                .mapToInt(FulfillmentPlan::getRequestedQuantity)
                .sum();
    }
    
    public int getTotalItemsFulfilled() {
        return fulfillmentPlans.stream()
                .mapToInt(FulfillmentPlan::getTotalFulfilled)
                .sum();
    }
}