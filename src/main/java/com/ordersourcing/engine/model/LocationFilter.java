package com.ordersourcing.engine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "location_filter")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationFilter {
    
    @Id
    private String id; // e.g., "SDD_FILTER_RULE", "ELECTRONICS_SECURE_RULE"
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String filterScript; // Multi-line AviatorScript for complex business logic
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_by")
    private String createdBy; // Business analyst who created the filter
    
    @Column(name = "last_modified")
    private LocalDateTime lastModified;
    
    @Column(name = "version")
    private String version; // For audit/rollback purposes
    
    @Column(name = "category")
    private String category; // e.g., "DELIVERY_TYPE", "PRODUCT_SECURITY", "CAPACITY"
    
    @Column(name = "execution_priority")
    private Integer executionPriority = 1; // Lower number = higher priority
    
    @Column(name = "cache_ttl_minutes")
    private Integer cacheTtlMinutes = 60; // Cache time-to-live in minutes
    
    // Scoring-related fields
    @Lob
    @Column(name = "scoring_script", columnDefinition = "TEXT")
    private String scoringScript; // AviatorScript for custom scoring logic
    
    @Column(name = "scoring_weights", columnDefinition = "TEXT")
    private String scoringWeights; // JSON string for weight overrides
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastModified = LocalDateTime.now();
    }
    
    public LocationFilter(String id, String name, String filterScript) {
        this.id = id;
        this.name = name;
        this.filterScript = filterScript;
        this.isActive = true;
        this.lastModified = LocalDateTime.now();
    }
}