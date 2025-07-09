package com.ordersourcing.engine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "scoring_configuration")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoringConfiguration {
    
    @Id
    private String id; // e.g., "DEFAULT_SCORING", "ELECTRONICS_PREMIUM_SCORING"
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "category")
    private String category; // e.g., "STANDARD", "PREMIUM", "ELECTRONICS", "HAZMAT"
    
    // Location scoring weights
    @Column(name = "transit_time_weight")
    private Double transitTimeWeight = -10.0; // Negative because longer transit is worse
    
    @Column(name = "processing_time_weight")
    private Double processingTimeWeight = -5.0; // Negative because longer processing is worse
    
    @Column(name = "inventory_weight")
    private Double inventoryWeight = 50.0; // Positive because more inventory is better
    
    @Column(name = "express_weight")
    private Double expressWeight = 20.0; // Bonus for express-capable locations
    
    // Split penalty configuration
    @Column(name = "split_penalty_base")
    private Double splitPenaltyBase = 15.0; // Base penalty for splitting orders
    
    @Column(name = "split_penalty_exponent")
    private Double splitPenaltyExponent = 1.5; // Exponential growth factor
    
    @Column(name = "split_penalty_multiplier")
    private Double splitPenaltyMultiplier = 10.0; // Multiplier for exponential penalty
    
    // Value-based adjustments
    @Column(name = "high_value_threshold")
    private Double highValueThreshold = 500.0; // Dollar threshold for high-value items
    
    @Column(name = "high_value_penalty")
    private Double highValuePenalty = 20.0; // Additional penalty for high-value items
    
    // Urgency-based adjustments
    @Column(name = "same_day_penalty")
    private Double sameDayPenalty = 25.0; // Additional penalty for same-day delivery
    
    @Column(name = "next_day_penalty")
    private Double nextDayPenalty = 15.0; // Additional penalty for next-day delivery
    
    // Distance-based scoring
    @Column(name = "distance_weight")
    private Double distanceWeight = -0.5; // Negative weight per kilometer
    
    @Column(name = "distance_threshold")
    private Double distanceThreshold = 100.0; // Distance threshold in kilometers
    
    // Confidence scoring adjustments
    @Column(name = "base_confidence")
    private Double baseConfidence = 0.8; // Base confidence score (0.0 to 1.0)
    
    @Column(name = "peak_season_adjustment")
    private Double peakSeasonAdjustment = -0.1; // Confidence reduction during peak season
    
    @Column(name = "weather_adjustment")
    private Double weatherAdjustment = -0.05; // Confidence reduction during weather events
    
    @Column(name = "hazmat_adjustment")
    private Double hazmatAdjustment = -0.15; // Confidence reduction for hazmat items
    
    // Execution priority for scoring configuration
    @Column(name = "execution_priority")
    private Integer executionPriority = 1; // Lower number = higher priority
    
    public ScoringConfiguration(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isActive = true;
    }
    
    public ScoringConfiguration(String id, String name, String description, String category) {
        this(id, name, description);
        this.category = category;
    }
}