package com.ordersourcing.engine.dto;

import com.ordersourcing.engine.model.FulfillmentPlan;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourcingResponse {
    
    private String tempOrderId;
    private List<EnhancedFulfillmentPlan> fulfillmentPlans;
    private SourcingMetadata metadata;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnhancedFulfillmentPlan {
        // Order Item Information
        private String sku;
        private Integer requestedQuantity;
        private String deliveryType;
        private String locationFilterId;
        
        // Fulfillment Summary
        private Integer totalFulfilled;
        private Integer remainingQuantity; // requestedQuantity - totalFulfilled
        private Boolean isPartialFulfillment;
        private Boolean isMultiLocationFulfillment;
        private Boolean isBackorder;
        
        // All Possible Fulfillment Locations (ordered by allocation priority)
        private List<LocationFulfillment> locationFulfillments;
        
        // Optimization Scores
        private Double overallScore; // Best strategy score (after penalties)
        private Double splitPenalty; // Penalty for multi-location if applicable
        private String recommendedStrategy; // "SINGLE_LOCATION" or "MULTI_LOCATION"
        private String scoringFactors;
        private List<String> warnings;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationFulfillment {
        // Location Details
        private LocationInfo location;
        
        // Inventory and Allocation
        private Integer availableInventory;
        private Integer allocatedQuantity; // How much is allocated from this location
        private Boolean canFulfillCompletely; // Can this location fulfill entire request alone?
        
        // Timing and Delivery
        private PromiseDateBreakdown promiseDates;
        
        // Scoring and Priority
        private Double locationScore;
        private Integer allocationPriority; // 1 = first choice, 2 = second choice, etc.
        private Boolean isPrimaryLocation; // True for the best location
        private Boolean isAllocatedInOptimalPlan; // True if this location is used in the recommended plan
        
        // Status and Warnings
        private String fulfillmentStatus; // "FULL", "PARTIAL", "AVAILABLE_NOT_USED"
        private List<String> warnings;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationInfo {
        private Integer id;
        private String name;
        private String type; // WAREHOUSE, STORE, DC
        private Double latitude;
        private Double longitude;
        private Integer transitTimeDays;
        private Integer processingTimeHours;
        private List<String> capabilities;
        private Double distanceFromCustomer;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuantityPromise {
        private Integer quantity;
        private LocalDateTime promiseDate;
        private LocalDateTime estimatedShipDate;
        private LocalDateTime estimatedDeliveryDate;
        private String batchInfo; // Which processing batch this quantity belongs to
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourcingMetadata {
        private Long totalProcessingTimeMs;
        private Long filterExecutionTimeMs;
        private Long inventoryFetchTimeMs;
        private Long promiseDateCalculationTimeMs;
        private Integer totalLocationsEvaluated;
        private Integer filtersExecuted;
        private Map<String, String> cacheHitInfo;
        private String processsingStrategy; // BATCH, SEQUENTIAL
        private List<String> performanceWarnings;
        private LocalDateTime requestTimestamp;
        private LocalDateTime responseTimestamp;
    }
    
    // Helper methods
    public boolean hasPartialFulfillments() {
        return fulfillmentPlans.stream()
                .anyMatch(plan -> plan.getIsPartialFulfillment() != null && plan.getIsPartialFulfillment());
    }
    
    public boolean hasBackorders() {
        return fulfillmentPlans.stream()
                .anyMatch(plan -> plan.getIsBackorder() != null && plan.getIsBackorder());
    }
    
    public boolean hasWarnings() {
        return fulfillmentPlans.stream()
                .anyMatch(plan -> plan.getWarnings() != null && !plan.getWarnings().isEmpty()) ||
               (metadata.getPerformanceWarnings() != null && !metadata.getPerformanceWarnings().isEmpty());
    }
    
    public double getAverageLocationScore() {
        return fulfillmentPlans.stream()
                .filter(plan -> plan.getOverallScore() != null)
                .mapToDouble(EnhancedFulfillmentPlan::getOverallScore)
                .average()
                .orElse(0.0);
    }
    
    public double getAveragePrimaryLocationScore() {
        return fulfillmentPlans.stream()
                .filter(plan -> plan.getLocationFulfillments() != null && !plan.getLocationFulfillments().isEmpty())
                .mapToDouble(plan -> plan.getLocationFulfillments().stream()
                        .filter(lf -> lf.getIsPrimaryLocation() != null && lf.getIsPrimaryLocation())
                        .mapToDouble(LocationFulfillment::getLocationScore)
                        .findFirst()
                        .orElse(0.0))
                .average()
                .orElse(0.0);
    }
}