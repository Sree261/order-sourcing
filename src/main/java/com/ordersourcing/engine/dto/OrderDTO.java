package com.ordersourcing.engine.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    
    @NotBlank(message = "Temporary order ID is required")
    private String tempOrderId; // Temporary ID for session tracking
    
    @NotNull(message = "Customer latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;
    
    @NotNull(message = "Customer longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;
    
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemDTO> orderItems;
    
    // Customer information for advanced filtering
    private String customerId;
    private String customerTier; // PREMIUM, STANDARD, BASIC
    private String zipCode;
    private String city;
    private String state;
    private String country;
    
    // Order-level preferences
    private Boolean allowPartialShipments;
    private Boolean allowBackorders;
    private Boolean preferSingleLocation;
    private LocalDateTime requestedDeliveryDate;
    private String orderType; // WEB, MOBILE, PHONE, B2B
    
    // Priority and timing
    private Integer orderPriority = 1; // 1=highest, 5=lowest
    private LocalDateTime requestTimestamp;
    private String timeZone;
    
    // Business context
    private String salesChannel; // ECOMMERCE, RETAIL, B2B
    private String campaignCode; // For promotional orders
    private Boolean isPeakSeason; // For peak season handling
    
    public OrderDTO(String tempOrderId, Double latitude, Double longitude, List<OrderItemDTO> orderItems) {
        this.tempOrderId = tempOrderId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.orderItems = orderItems;
        this.requestTimestamp = LocalDateTime.now();
        this.orderPriority = 1;
        this.allowPartialShipments = true;
        this.allowBackorders = false;
        this.preferSingleLocation = true;
    }
    
    // Helper methods for business logic
    public int getTotalQuantity() {
        return orderItems.stream().mapToInt(OrderItemDTO::getQuantity).sum();
    }
    
    public boolean hasMultipleDeliveryTypes() {
        return orderItems.stream()
                .map(OrderItemDTO::getDeliveryType)
                .distinct()
                .count() > 1;
    }
    
    public boolean isLargeOrder() {
        return getTotalQuantity() > 10 || orderItems.size() > 5;
    }
    
    public boolean hasTimeSensitiveItems() {
        return orderItems.stream().anyMatch(OrderItemDTO::isTimeSensitive);
    }
    
    public boolean hasHighSecurityItems() {
        return orderItems.stream().anyMatch(OrderItemDTO::requiresHighSecurity);
    }
    
    @Override
    public String toString() {
        return String.format("OrderDTO{tempOrderId='%s', items=%d, totalQty=%d, location=[%.4f,%.4f]}", 
                           tempOrderId, orderItems.size(), getTotalQuantity(), latitude, longitude);
    }
}