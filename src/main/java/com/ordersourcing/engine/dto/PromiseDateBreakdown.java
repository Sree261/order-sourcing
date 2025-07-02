package com.ordersourcing.engine.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromiseDateBreakdown {
    
    // Final promise date to customer
    private LocalDateTime promiseDate;
    
    // Detailed breakdown of calculation
    private LocalDateTime orderProcessingStart;
    private LocalDateTime orderProcessingComplete;
    private LocalDateTime locationProcessingStart;
    private LocalDateTime locationProcessingComplete;
    private LocalDateTime carrierPickupTime;
    private LocalDateTime estimatedDeliveryDate;
    
    // Time components (in hours)
    private Integer systemProcessingHours;
    private Integer locationProcessingHours;
    private Integer carrierTransitHours;
    private Integer bufferHours;
    
    // Business logic details
    private String carrierCode; // UPS, FEDEX, USPS
    private String serviceLevel; // GROUND, EXPRESS, OVERNIGHT
    private String deliveryType; // SAME_DAY, NEXT_DAY, STANDARD
    private Boolean isBusinessDaysOnly;
    private Boolean isWeatherAdjusted;
    private Boolean isPeakSeasonAdjusted;
    
    // Confidence and alternatives
    private Double confidenceScore; // 0.0 to 1.0
    private String riskFactors; // Weather, peak season, capacity
    private LocalDateTime earliestPossibleDate;
    private LocalDateTime latestAcceptableDate;
    
    // Performance tracking
    private Long calculationTimeMs;
    private String calculationMethod; // CACHED, COMPUTED, FALLBACK
    
    public static PromiseDateBreakdown createFallback(LocalDateTime fallbackDate, String reason) {
        return PromiseDateBreakdown.builder()
                .promiseDate(fallbackDate)
                .confidenceScore(0.5)
                .riskFactors(reason)
                .calculationMethod("FALLBACK")
                .build();
    }
    
    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore >= 0.8;
    }
    
    public boolean hasRisks() {
        return riskFactors != null && !riskFactors.trim().isEmpty();
    }
}