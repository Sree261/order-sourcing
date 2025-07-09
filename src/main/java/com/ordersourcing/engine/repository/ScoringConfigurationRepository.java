package com.ordersourcing.engine.repository;

import com.ordersourcing.engine.model.ScoringConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoringConfigurationRepository extends JpaRepository<ScoringConfiguration, String> {
    
    List<ScoringConfiguration> findByIsActiveTrueOrderByExecutionPriorityAsc();
    
    List<ScoringConfiguration> findByCategoryAndIsActiveTrue(String category);
    
    @Query("SELECT sc FROM ScoringConfiguration sc WHERE sc.id IN :configIds AND sc.isActive = true")
    List<ScoringConfiguration> findActiveConfigurationsByIds(@Param("configIds") List<String> configIds);
    
    Optional<ScoringConfiguration> findByIdAndIsActiveTrue(String id);
    
    
    @Query("SELECT sc FROM ScoringConfiguration sc WHERE sc.category = :category " +
           "AND sc.isActive = true ORDER BY sc.executionPriority ASC")
    List<ScoringConfiguration> findByCategoryOrderByPriority(@Param("category") String category);
    
    
    Optional<ScoringConfiguration> findFirstByIsActiveTrueOrderByExecutionPriorityAsc();
}