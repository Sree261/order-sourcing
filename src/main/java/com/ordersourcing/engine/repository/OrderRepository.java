package com.ordersourcing.engine.repository;

import com.ordersourcing.engine.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}
