package com.ordersourcing.engine.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {
    
    @NotBlank(message = "SKU is required")
    private String sku;
    
    @Positive(message = "Quantity must be positive")
    private int quantity;
    
    @NotBlank(message = "Delivery type is required")
    private String deliveryType; // SAME_DAY, NEXT_DAY, STANDARD, SHIP_FROM_STORE
    
    @NotBlank(message = "Location filter ID is required")
    private String locationFilterId; // e.g., "SDD_FILTER_RULE", "ELECTRONICS_SECURE_RULE"
    
    // Additional metadata for filtering and promise date calculation
    private String productCategory; // For category-specific filtering
    private Double unitPrice; // For value-based security filtering
    private Boolean isHazmat; // For hazmat filtering
    private Boolean requiresColdStorage; // For temperature-controlled items
    private String specialHandling; // Additional handling requirements
    
    // Customer preferences (for advanced filtering)
    private Boolean customerPrefersSingleLocation;
    private Integer maxAcceptableTransitDays;
    
    // Fulfillment policies (item-level controls)
    private Boolean allowPartialFulfillment; // Allow partial fulfillment for this item
    private Boolean allowBackorder; // Allow backorder for this item
    private Boolean preferSingleLocation; // Prefer single location for this item
    private Boolean requireFullQuantity; // Must fulfill complete quantity or nothing
    
    // For promise date calculation
    private String carrierPreference; // UPS, FEDEX, USPS, etc.
    private Boolean isExpressPriority;
    
    @Override
    public String toString() {
        return String.format("OrderItemDTO{sku='%s', quantity=%d, deliveryType='%s', locationFilterId='%s'}", 
                           sku, quantity, deliveryType, locationFilterId);
    }
    
    // Helper method to check if item needs special security
    public boolean requiresHighSecurity() {
        return (unitPrice != null && unitPrice > 1000.0) || 
               (productCategory != null && (productCategory.startsWith("ELECTRONICS") || 
                                          productCategory.startsWith("JEWELRY")));
    }
    
    // Helper method to check if item is time-sensitive
    public boolean isTimeSensitive() {
        return "SAME_DAY".equals(deliveryType) || 
               "NEXT_DAY".equals(deliveryType) || 
               requiresColdStorage != null && requiresColdStorage;
    }
}