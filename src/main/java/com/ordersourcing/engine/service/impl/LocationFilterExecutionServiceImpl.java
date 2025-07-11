package com.ordersourcing.engine.service.impl;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorDouble;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.ordersourcing.engine.dto.OrderDTO;
import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.model.Location;
import com.ordersourcing.engine.model.LocationFilter;
import com.ordersourcing.engine.repository.LocationFilterRepository;
import com.ordersourcing.engine.repository.LocationRepository;
import com.ordersourcing.engine.service.LocationFilterExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class LocationFilterExecutionServiceImpl implements LocationFilterExecutionService {
    
    @Autowired
    private LocationFilterRepository locationFilterRepository;
    
    @Autowired
    private LocationRepository locationRepository;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    // Pre-computed filter results cache
    private final Map<String, Set<Integer>> precomputedResults = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> precomputedTimestamps = new ConcurrentHashMap<>();
    
    // Compiled expression cache
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    
    
    /**
     * Execute location filter with intelligent caching
     */
    @Cacheable(value = "locationFilters", key = "#filterId + ':' + #orderContext.hashCode()")
    public List<Location> executeLocationFilter(String filterId, OrderDTO orderContext) {
        try {
            // Try pre-computed results first (fastest path)
            Optional<List<Location>> precomputed = tryPrecomputedResults(filterId);
            if (precomputed.isPresent()) {
                return precomputed.get();
            }
            
            // Get filter configuration
            Optional<LocationFilter> filterOpt = locationFilterRepository.findByIdAndIsActiveTrue(filterId);
            if (filterOpt.isEmpty()) {
                log.warn("Location filter not found or inactive: {}", filterId);
                return Collections.emptyList();
            }
            
            LocationFilter filter = filterOpt.get();
            
            // Execute script on all locations
            return executeFilterScript(filter, orderContext);
            
        } catch (Exception e) {
            log.error("Error executing location filter: {}", filterId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Batch execute multiple filters in parallel
     */
    public CompletableFuture<Map<String, List<Location>>> batchExecuteFilters(
            Set<String> filterIds, OrderDTO orderContext) {
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, List<Location>> results = new ConcurrentHashMap<>();
            
            filterIds.parallelStream().forEach(filterId -> {
                try {
                    List<Location> locations = applicationContext.getBean(LocationFilterExecutionService.class)
                            .executeLocationFilter(filterId, orderContext);
                    results.put(filterId, locations);
                } catch (Exception e) {
                    log.error("Error in batch filter execution for filter: {}", filterId, e);
                    results.put(filterId, Collections.emptyList());
                }
            });
            
            return results;
        });
    }
    
    
    
    /**
     * Execute filter script with enhanced context
     */
    private List<Location> executeFilterScript(LocationFilter filter, OrderDTO orderContext) {
        List<Location> allLocations = locationRepository.findAll();
        List<Location> filteredLocations = new ArrayList<>();
        
        // Get or compile expression
        Expression compiledExpression = getCompiledExpression(filter);
        if (compiledExpression == null) {
            return Collections.emptyList();
        }
        
        for (Location location : allLocations) {
            try {
                Map<String, Object> env = createExecutionEnvironment(location, orderContext, null);
                Boolean result = (Boolean) compiledExpression.execute(env);
                
                if (result != null && result) {
                    filteredLocations.add(location);
                }
            } catch (Exception e) {
                log.warn("Filter execution failed for location {} with filter {}: {}", 
                        location.getId(), filter.getId(), e.getMessage());
            }
        }
        
        return filteredLocations;
    }
    
    /**
     * Create rich execution environment for script
     */
    private Map<String, Object> createExecutionEnvironment(Location location, OrderDTO orderContext, OrderItemDTO orderItem) {
        Map<String, Object> env = new HashMap<>();
        
        // Location context - used in actual filter scripts
        env.put("location", location);
        
        // Order context - used in actual filter scripts  
        env.put("order", orderContext);
        
        return env;
    }
    
    
    /**
     * Get compiled expression with caching
     */
    private Expression getCompiledExpression(LocationFilter filter) {
        try {
            return expressionCache.computeIfAbsent(filter.getId(), 
                    k -> AviatorEvaluator.compile(filter.getFilterScript()));
        } catch (Exception e) {
            log.error("Failed to compile filter script for filter: {}", filter.getId(), e);
            return null;
        }
    }
    
    /**
     * Try to get pre-computed results
     */
    private Optional<List<Location>> tryPrecomputedResults(String filterId) {
        Set<Integer> locationIds = precomputedResults.get(filterId);
        LocalDateTime timestamp = precomputedTimestamps.get(filterId);
        
        if (locationIds != null && timestamp != null) {
            // Check if cache is still valid (using filter's TTL)
            LocationFilter filter = locationFilterRepository.findById(filterId).orElse(null);
            if (filter != null) {
                long cacheAgeMinutes = java.time.temporal.ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now());
                if (cacheAgeMinutes < filter.getCacheTtlMinutes()) {
                    log.debug("Using pre-computed results for filter: {}", filterId);
                    return Optional.of(locationRepository.findAllById(locationIds));
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Initialize filter service
     */
    @PostConstruct
    public void initialize() {
        // Register custom functions with Aviator
        AviatorEvaluator.addFunction(new CalculateDistanceFunction());
    }

    
    /**
     * Custom Aviator function for calculating distance
     */
    public static class CalculateDistanceFunction extends AbstractFunction {
        @Override
        public String getName() {
            return "calculateDistance";
        }
        
        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3, AviatorObject arg4) {
            double lat1 = FunctionUtils.getNumberValue(arg1, env).doubleValue();
            double lon1 = FunctionUtils.getNumberValue(arg2, env).doubleValue();
            double lat2 = FunctionUtils.getNumberValue(arg3, env).doubleValue();
            double lon2 = FunctionUtils.getNumberValue(arg4, env).doubleValue();
            
            double distance = Math.sqrt(Math.pow(lat1 - lat2, 2) + Math.pow(lon1 - lon2, 2)) * 111.32;
            return new AviatorDouble(distance);
        }
    }
}