package com.ordersourcing.engine.repository;

import com.ordersourcing.engine.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {
    List<Inventory> findBySkuAndQuantityGreaterThan(String sku, int quantity);
    
    @Query("SELECT i FROM Inventory i WHERE i.sku IN :skus AND i.quantity > 0 ORDER BY i.quantity DESC")
    List<Inventory> findBySkusWithStock(@Param("skus") List<String> skus);
}
