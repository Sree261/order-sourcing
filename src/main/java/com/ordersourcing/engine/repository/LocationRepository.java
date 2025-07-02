package com.ordersourcing.engine.repository;

import com.ordersourcing.engine.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Integer> {
    @Override
    @Cacheable("locations")
    List<Location> findAll();
}
