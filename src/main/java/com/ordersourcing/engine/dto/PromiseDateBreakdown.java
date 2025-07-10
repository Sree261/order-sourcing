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
    
    // Core promise date information
    private LocalDateTime promiseDate;
    private LocalDateTime carrierPickupTime; // Used as estimatedShipDate in API
    private LocalDateTime estimatedDeliveryDate;
    
    // Time components (in hours)
    private Integer locationProcessingHours;
    private Integer carrierTransitHours;
    
    // Carrier information
    private String carrierCode; // UPS, FEDEX, USPS
    private String serviceLevel; // GROUND, EXPRESS, OVERNIGHT
    private String deliveryType; // SAME_DAY, NEXT_DAY, STANDARD
    
    public static PromiseDateBreakdown createFallback(LocalDateTime fallbackDate, String reason) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime estimatedShip = now.plusHours(24); // Default 24 hour processing
        LocalDateTime estimatedDelivery = fallbackDate.minusDays(1); // 1 day before promise
        
        return PromiseDateBreakdown.builder()
                .promiseDate(fallbackDate)
                .carrierPickupTime(estimatedShip)
                .estimatedDeliveryDate(estimatedDelivery)
                .locationProcessingHours(24)
                .carrierTransitHours(24)
                .carrierCode("FALLBACK")
                .serviceLevel("STANDARD")
                .deliveryType("STANDARD")
                .build();
    }
}