package com.ordersourcing.engine.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String orderId;
    private String country;
    private String city;
    private String state;
    private double latitude;
    private double longitude;

    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;
}
