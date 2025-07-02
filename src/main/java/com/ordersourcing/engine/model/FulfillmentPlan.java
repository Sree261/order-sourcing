package com.ordersourcing.engine.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class FulfillmentPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    private Order order;

    @ManyToOne
    private Location location;

    private String sku;
    private int quantity;

    // Promise date fields
    private LocalDateTime promiseDate; // When the item is promised to be delivered
    private LocalDateTime estimatedShipDate; // When the item is estimated to ship from location
    private LocalDateTime estimatedDeliveryDate; // When the item is estimated to be delivered

    // Additional promise-related information
    private String deliveryType; // Store delivery type for promise calculation
    private Integer processingTimeHours; // Processing time in hours
    private Integer transitTimeHours; // Transit time in hours
}
