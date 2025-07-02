package com.ordersourcing.engine.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String sku;
    private Integer locationId;
    private int quantity;
    private int processingTime;
}