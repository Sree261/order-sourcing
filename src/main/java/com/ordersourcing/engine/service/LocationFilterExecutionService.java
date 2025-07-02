package com.ordersourcing.engine.service;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.ordersourcing.engine.dto.OrderDTO;
import com.ordersourcing.engine.dto.OrderItemDTO;
import com.ordersourcing.engine.model.Location;
import com.ordersourcing.engine.model.LocationFilter;
import com.ordersourcing.engine.repository.LocationFilterRepository;
import com.ordersourcing.engine.repository.LocationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocationFilterExecutionService {
    
    @Autowired
    private LocationFilterRepository locationFilterRepository;
    
    @Autowired
    private LocationRepository locationRepository;
    
    // Pre-computed filter results cache
    private final Map<String, Set<Integer>> precomputedResults = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> precomputedTimestamps = new ConcurrentHashMap<>();
    
    // Compiled expression cache
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    
    // Performance metrics
    private final Map<String, FilterMetrics> filterMetrics = new ConcurrentHashMap<>();
    
    /**
     * Execute location filter with intelligent caching
     */
    @Cacheable(value = "locationFilters", key = "#filterId + ':' + #orderContext.hashCode()")
    public List<Location> executeLocationFilter(String filterId, OrderDTO orderContext) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Try pre-computed results first (fastest path)
            Optional<List<Location>> precomputed = tryPrecomputedResults(filterId);
            if (precomputed.isPresent()) {
                recordMetrics(filterId, System.currentTimeMillis() - startTime, "PRECOMPUTED");
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
            List<Location> result = executeFilterScript(filter, orderContext);
            
            recordMetrics(filterId, System.currentTimeMillis() - startTime, "COMPUTED");
            return result;
            
        } catch (Exception e) {
            log.error("Error executing location filter: {}", filterId, e);
            recordMetrics(filterId, System.currentTimeMillis() - startTime, "ERROR");
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
                    List<Location> locations = executeLocationFilter(filterId, orderContext);
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
                Map<String, Object> env = createExecutionEnvironment(location, orderContext);
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
    private Map<String, Object> createExecutionEnvironment(Location location, OrderDTO orderContext) {
        Map<String, Object> env = new HashMap<>();
        
        // Location context
        env.put("location", location);
        
        // Order context
        env.put("order", orderContext);
        
        // Time context
        LocalDateTime now = LocalDateTime.now();
        env.put("time", createTimeContext(now));
        
        // Math utilities
        env.put("math", new MathUtilities());
        
        // Distance utilities
        env.put("distance", new DistanceUtilities(orderContext.getLatitude(), orderContext.getLongitude()));
        
        // Business context
        env.put("business", new BusinessContext());
        
        return env;
    }
    
    private Map<String, Object> createTimeContext(LocalDateTime now) {
        Map<String, Object> timeContext = new HashMap<>();
        timeContext.put("hour", now.getHour());
        timeContext.put("dayOfWeek", now.getDayOfWeek().getValue());
        timeContext.put("month", now.getMonthValue());
        timeContext.put("isWeekend", now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY);
        timeContext.put("isBusinessHours", now.getHour() >= 9 && now.getHour() <= 17);
        return timeContext;
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
     * Pre-compute filter results for active filters
     */
    @PostConstruct
    public void precomputeActiveFilters() {
        // Run asynchronously to avoid blocking startup
        CompletableFuture.runAsync(this::refreshPrecomputedResults);
    }
    
    public void refreshPrecomputedResults() {
        try {
            List<LocationFilter> activeFilters = locationFilterRepository.findByIsActiveTrueOrderByExecutionPriorityAsc();
            log.info("Pre-computing results for {} active filters", activeFilters.size());
            
            for (LocationFilter filter : activeFilters) {
                try {
                    // Create a neutral order context for pre-computation
                    OrderDTO neutralContext = createNeutralOrderContext();
                    List<Location> results = executeFilterScript(filter, neutralContext);
                    
                    Set<Integer> locationIds = results.stream()
                            .map(Location::getId)
                            .collect(Collectors.toSet());
                    
                    precomputedResults.put(filter.getId(), locationIds);
                    precomputedTimestamps.put(filter.getId(), LocalDateTime.now());
                    
                    log.debug("Pre-computed {} locations for filter: {}", locationIds.size(), filter.getId());
                    
                } catch (Exception e) {
                    log.error("Failed to pre-compute results for filter: {}", filter.getId(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error in pre-computing filter results", e);
        }
    }
    
    private OrderDTO createNeutralOrderContext() {
        // Create a neutral context for pre-computation (without specific customer location)
        return OrderDTO.builder()
                .tempOrderId("PRECOMPUTE")
                .latitude(40.7128) // Default NYC coordinates
                .longitude(-74.0060)
                .orderItems(Collections.emptyList())
                .requestTimestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Invalidate cache for a specific filter
     */
    public void invalidateFilterCache(String filterId) {
        precomputedResults.remove(filterId);
        precomputedTimestamps.remove(filterId);
        expressionCache.remove(filterId);
        log.info("Invalidated cache for filter: {}", filterId);
    }
    
    /**
     * Get filter performance metrics
     */
    public Map<String, FilterMetrics> getFilterMetrics() {
        return new HashMap<>(filterMetrics);
    }
    
    private void recordMetrics(String filterId, long executionTimeMs, String method) {
        filterMetrics.compute(filterId, (k, v) -> {
            if (v == null) {
                v = new FilterMetrics();
            }
            v.recordExecution(executionTimeMs, method);
            return v;
        });
    }
    
    // Utility classes for script execution
    public static class MathUtilities {
        public double sqrt(double value) { return Math.sqrt(value); }
        public double pow(double base, double exponent) { return Math.pow(base, exponent); }
        public double abs(double value) { return Math.abs(value); }
        public double min(double a, double b) { return Math.min(a, b); }
        public double max(double a, double b) { return Math.max(a, b); }
        public long ceil(double value) { return Math.round(Math.ceil(value)); }
        public long floor(double value) { return Math.round(Math.floor(value)); }
    }
    
    public static class DistanceUtilities {
        private final double customerLat;
        private final double customerLon;
        
        public DistanceUtilities(double customerLat, double customerLon) {
            this.customerLat = customerLat;
            this.customerLon = customerLon;
        }
        
        public double calculate(double locationLat, double locationLon) {
            return Math.sqrt(Math.pow(locationLat - customerLat, 2) + 
                           Math.pow(locationLon - customerLon, 2)) * 111.32; // Approximate km
        }
        
        public boolean isWithinRadius(double locationLat, double locationLon, double radiusKm) {
            return calculate(locationLat, locationLon) <= radiusKm;
        }
    }
    
    public static class BusinessContext {
        public boolean isPeakSeason() {
            int month = LocalDateTime.now().getMonthValue();
            return month == 11 || month == 12; // November-December
        }
        
        public boolean isHoliday() {
            // Simplified holiday check - in real implementation, would use holiday calendar
            LocalDateTime now = LocalDateTime.now();
            return (now.getMonthValue() == 12 && now.getDayOfMonth() == 25) || // Christmas
                   (now.getMonthValue() == 1 && now.getDayOfMonth() == 1);     // New Year
        }
        
        public String getCurrentTimeZone() {
            return "UTC"; // Simplified - in real implementation, would determine based on customer location
        }
    }
    
    public static class FilterMetrics {
        private long totalExecutions = 0;
        private long totalExecutionTimeMs = 0;
        private long precomputedHits = 0;
        private long computedExecutions = 0;
        private long errors = 0;
        private LocalDateTime lastExecution;
        
        public void recordExecution(long executionTimeMs, String method) {
            totalExecutions++;
            totalExecutionTimeMs += executionTimeMs;
            lastExecution = LocalDateTime.now();
            
            switch (method) {
                case "PRECOMPUTED":
                    precomputedHits++;
                    break;
                case "COMPUTED":
                    computedExecutions++;
                    break;
                case "ERROR":
                    errors++;
                    break;
            }
        }
        
        public double getAverageExecutionTimeMs() {
            return totalExecutions > 0 ? (double) totalExecutionTimeMs / totalExecutions : 0;
        }
        
        public double getCacheHitRate() {
            return totalExecutions > 0 ? (double) precomputedHits / totalExecutions : 0;
        }
        
        // Getters
        public long getTotalExecutions() { return totalExecutions; }
        public long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }
        public long getPrecomputedHits() { return precomputedHits; }
        public long getComputedExecutions() { return computedExecutions; }
        public long getErrors() { return errors; }
        public LocalDateTime getLastExecution() { return lastExecution; }
    }
}