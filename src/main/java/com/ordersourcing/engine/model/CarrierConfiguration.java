package com.ordersourcing.engine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "carrier_configuration")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarrierConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false)
    private String carrierCode; // UPS, FEDEX, USPS, DHL
    
    @Column(nullable = false)
    private String serviceLevel; // GROUND, EXPRESS, OVERNIGHT, SAME_DAY
    
    @Column(nullable = false)
    private String deliveryType; // SAME_DAY, NEXT_DAY, STANDARD
    
    // Transit time configurations
    private Integer baseTransitDays;
    private Integer maxTransitDays;
    private Double transitTimeMultiplier = 1.0; // For distance-based adjustments
    
    // Pickup and delivery schedules
    @Column(name = "pickup_cutoff_time")
    private LocalTime pickupCutoffTime; // e.g., 15:00 for 3 PM cutoff
    
    @Column(name = "next_pickup_time")
    private LocalTime nextPickupTime; // Next available pickup after cutoff
    
    @Column(name = "delivery_start_time")
    private LocalTime deliveryStartTime; // e.g., 09:00
    
    @Column(name = "delivery_end_time")
    private LocalTime deliveryEndTime; // e.g., 18:00
    
    // Service capabilities
    private Boolean weekendPickup = false;
    private Boolean weekendDelivery = false;
    private Boolean holidayService = false;
    private Boolean residentialDelivery = true;
    private Boolean signatureRequired = false;
    
    // Distance and zone limitations
    private Double maxDistanceKm; // Maximum service distance
    private String serviceZones; // Comma-separated zone codes
    
    // Pricing and priority
    private Double baseCost;
    private Integer carrierPriority = 1; // Lower number = higher priority
    
    // Special handling
    private Boolean supportsHazmat = false;
    private Boolean supportsColdChain = false;
    private Boolean supportsHighValue = false;
    private Double maxValueLimit; // Maximum insured value
    
    // Performance metrics
    private Double onTimePerformance; // 0.0 to 1.0
    private Integer averageDelayHours;
    
    // Seasonal adjustments
    private Boolean isPeakSeasonService = true;
    private Integer peakSeasonDelayDays = 0;
    
    public CarrierConfiguration(String carrierCode, String serviceLevel, String deliveryType, 
                              Integer baseTransitDays, LocalTime pickupCutoffTime) {
        this.carrierCode = carrierCode;
        this.serviceLevel = serviceLevel;
        this.deliveryType = deliveryType;
        this.baseTransitDays = baseTransitDays;
        this.pickupCutoffTime = pickupCutoffTime;
        this.deliveryStartTime = LocalTime.of(9, 0);
        this.deliveryEndTime = LocalTime.of(18, 0);
        this.onTimePerformance = 0.95;
    }
}