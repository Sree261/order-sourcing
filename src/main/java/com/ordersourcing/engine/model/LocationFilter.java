package com.ordersourcing.engine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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

    @Column(columnDefinition = "TEXT")
    private String filterScript; // Multi-line AviatorScript for complex business logic
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "category")
    private String category; // e.g., "DELIVERY_TYPE", "PRODUCT_SECURITY", "CAPACITY"
    
    @Column(name = "execution_priority")
    private Integer executionPriority = 1; // Lower number = higher priority
    
    @Column(name = "cache_ttl_minutes")
    private Integer cacheTtlMinutes = 60; // Cache time-to-live in minutes
    
    public LocationFilter(String id, String name, String filterScript) {
        this.id = id;
        this.name = name;
        this.filterScript = filterScript;
        this.isActive = true;
    }
}