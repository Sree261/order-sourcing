package com.ordersourcing.engine.repository;

import com.ordersourcing.engine.model.CarrierConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarrierConfigurationRepository extends JpaRepository<CarrierConfiguration, Integer> {
    
    List<CarrierConfiguration> findByDeliveryTypeOrderByCarrierPriorityAsc(String deliveryType);
    
    List<CarrierConfiguration> findByCarrierCodeAndDeliveryType(String carrierCode, String deliveryType);
    
    Optional<CarrierConfiguration> findByCarrierCodeAndServiceLevelAndDeliveryType(
            String carrierCode, String serviceLevel, String deliveryType);
    
    @Query("SELECT cc FROM CarrierConfiguration cc WHERE cc.deliveryType = :deliveryType " +
           "AND (cc.maxDistanceKm IS NULL OR cc.maxDistanceKm >= :distance) " +
           "ORDER BY cc.carrierPriority ASC")
    List<CarrierConfiguration> findByDeliveryTypeAndMaxDistance(
            @Param("deliveryType") String deliveryType, 
            @Param("distance") Double distance);
    
    @Query("SELECT cc FROM CarrierConfiguration cc WHERE cc.deliveryType = :deliveryType " +
           "AND cc.weekendDelivery = :weekendRequired " +
           "ORDER BY cc.carrierPriority ASC")
    List<CarrierConfiguration> findByDeliveryTypeAndWeekendService(
            @Param("deliveryType") String deliveryType, 
            @Param("weekendRequired") Boolean weekendRequired);
    
    @Query("SELECT cc FROM CarrierConfiguration cc WHERE cc.deliveryType = :deliveryType " +
           "AND cc.supportsHazmat = true ORDER BY cc.carrierPriority ASC")
    List<CarrierConfiguration> findHazmatCapableCarriers(@Param("deliveryType") String deliveryType);
    
    @Query("SELECT cc FROM CarrierConfiguration cc WHERE cc.deliveryType = :deliveryType " +
           "AND cc.supportsColdChain = true ORDER BY cc.carrierPriority ASC")
    List<CarrierConfiguration> findColdChainCapableCarriers(@Param("deliveryType") String deliveryType);
}