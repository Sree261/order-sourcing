package com.ordersourcing.engine.repository;

import com.ordersourcing.engine.model.LocationFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationFilterRepository extends JpaRepository<LocationFilter, String> {
    
    List<LocationFilter> findByIsActiveTrueOrderByExecutionPriorityAsc();
    
    List<LocationFilter> findByCategoryAndIsActiveTrue(String category);
    
    @Query("SELECT lf FROM LocationFilter lf WHERE lf.id IN :filterIds AND lf.isActive = true")
    List<LocationFilter> findActiveFiltersByIds(@Param("filterIds") List<String> filterIds);
    
    Optional<LocationFilter> findByIdAndIsActiveTrue(String id);
    
    @Query("SELECT lf FROM LocationFilter lf WHERE lf.lastModified > :since AND lf.isActive = true")
    List<LocationFilter> findRecentlyModified(@Param("since") java.time.LocalDateTime since);
}