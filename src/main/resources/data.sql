-- Insert sample LocationFilter data with simplified scripts

-- Same Day Delivery Filter
INSERT INTO location_filter (id, name, description, filter_script, is_active, created_by, version, category, execution_priority, cache_ttl_minutes) VALUES 
('SDD_FILTER_RULE', 'Same Day Delivery Filter', 'Complex filter for same day delivery eligibility', 
'true', 
true, 'system', '1.0', 'DELIVERY_TYPE', 1, 30);

-- Electronics Security Filter
INSERT INTO location_filter (id, name, description, filter_script, is_active, created_by, version, category, execution_priority, cache_ttl_minutes) VALUES
('ELECTRONICS_SECURE_RULE', 'Electronics Security Filter', 'Security requirements for electronics items',
'calculateDistance(location.latitude, location.longitude, order.latitude, order.longitude) <= 50', 
true, 'security_team', '1.0', 'PRODUCT_SECURITY', 1, 60);

-- Peak Season Capacity Filter  
INSERT INTO location_filter (id, name, description, filter_script, is_active, created_by, version, category, execution_priority, cache_ttl_minutes) VALUES
('PEAK_SEASON_RULE', 'Peak Season Capacity Filter', 'Dynamic capacity management for peak seasons',
'location.transitTime <= 3', 
true, 'operations_team', '1.0', 'CAPACITY', 2, 15);

-- Frozen Food Filter
INSERT INTO location_filter (id, name, description, filter_script, is_active, created_by, version, category, execution_priority, cache_ttl_minutes) VALUES
('FROZEN_FOOD_RULE', 'Frozen Food Storage Filter', 'Temperature-controlled storage requirements',
'calculateDistance(location.latitude, location.longitude, order.latitude, order.longitude) <= 30', 
true, 'cold_chain_team', '1.0', 'PRODUCT_CATEGORY', 1, 45);

-- Standard Delivery Filter  
INSERT INTO location_filter (id, name, description, filter_script, is_active, created_by, version, category, execution_priority, cache_ttl_minutes) VALUES
('STANDARD_DELIVERY_RULE', 'Standard Delivery Filter', 'Basic filter for standard delivery items',
'true', 
true, 'logistics_team', '1.0', 'DELIVERY_TYPE', 3, 120);

-- Insert sample CarrierConfiguration data
INSERT INTO carrier_configuration (carrier_code, service_level, delivery_type, base_transit_days, max_transit_days, transit_time_multiplier, pickup_cutoff_time, next_pickup_time, delivery_start_time, delivery_end_time, weekend_pickup, weekend_delivery, holiday_service, residential_delivery, signature_required, max_distance_km, base_cost, carrier_priority, supports_hazmat, supports_cold_chain, supports_high_value, max_value_limit, on_time_performance, average_delay_hours, is_peak_season_service, peak_season_delay_days) VALUES
('UPS', 'GROUND', 'STANDARD', 3, 5, 1.0, '17:00', '09:00', '09:00', '18:00', false, false, false, true, false, 1000, 15.99, 1, false, false, true, 5000, 0.95, 4, true, 1),
('UPS', 'EXPRESS', 'NEXT_DAY', 1, 2, 1.0, '15:00', '09:00', '09:00', '18:00', false, false, false, true, true, 500, 45.99, 1, false, false, true, 10000, 0.97, 2, true, 0),
('FEDEX', 'GROUND', 'STANDARD', 3, 5, 1.0, '17:00', '09:00', '09:00', '18:00', false, false, false, true, false, 1000, 16.99, 2, false, false, true, 5000, 0.94, 6, true, 1),
('FEDEX', 'OVERNIGHT', 'NEXT_DAY', 1, 1, 1.0, '15:00', '09:00', '08:00', '18:00', false, false, false, true, true, 800, 49.99, 2, true, true, true, 15000, 0.98, 1, true, 0),
('USPS', 'PRIORITY', 'STANDARD', 2, 3, 1.0, '17:00', '09:00', '09:00', '17:00', false, true, false, true, false, 2000, 12.99, 3, false, false, false, 1000, 0.88, 12, false, 2),
('LOCAL_COURIER', 'SAME_DAY', 'SAME_DAY', 0, 0, 1.0, '20:00', '06:00', '06:00', '22:00', true, true, false, true, false, 50, 25.99, 1, false, true, false, 2000, 0.92, 1, true, 0);

-- Insert some sample locations for testing (these would typically come from your existing location data)
INSERT INTO location (id, name, latitude, longitude, transit_time) VALUES
(1, 'Downtown Warehouse', 40.7128, -74.0060, 1),
(2, 'Suburb Store', 40.7489, -73.9857, 2),
(3, 'Airport Distribution Center', 40.6892, -74.1745, 1),
(4, 'Regional Hub', 41.8781, -87.6298, 3),
(5, 'Express Center', 40.7831, -73.9712, 1);

-- H2 will automatically manage the sequence

-- Insert sample inventory data
INSERT INTO inventory (location_id, sku, quantity, processing_time) VALUES
(1, 'PHONE123', 50, 1),
(1, 'LAPTOP456', 25, 1),
(1, 'TABLET789', 30, 1),
(2, 'PHONE123', 20, 2),
(2, 'HEADPHONES101', 100, 1),
(3, 'LAPTOP456', 15, 1),
(3, 'TABLET789', 40, 1),
(4, 'PHONE123', 75, 2),
(4, 'LAPTOP456', 35, 2),
(5, 'TABLET789', 60, 1),
(5, 'HEADPHONES101', 80, 1);

-- Insert sample ScoringConfiguration data

-- Default Scoring Configuration
INSERT INTO scoring_configuration (
    id, name, description, is_active, created_by, version, category, 
    transit_time_weight, processing_time_weight, inventory_weight, express_weight,
    split_penalty_base, split_penalty_exponent, split_penalty_multiplier,
    high_value_threshold, high_value_penalty, same_day_penalty, next_day_penalty,
    distance_weight, distance_threshold, base_confidence, peak_season_adjustment,
    weather_adjustment, hazmat_adjustment, execution_priority, cache_ttl_minutes
) VALUES (
    'DEFAULT_SCORING', 'Default Scoring Configuration', 
    'Standard scoring weights for general order fulfillment', 
    true, 'system', '1.0', 'DEFAULT',
    -10.0, -5.0, 50.0, 20.0,
    15.0, 1.5, 10.0,
    500.0, 20.0, 25.0, 15.0,
    -0.5, 100.0, 0.8, -0.1,
    -0.05, -0.15, 1, 60
);

-- Electronics Premium Scoring Configuration
INSERT INTO scoring_configuration (
    id, name, description, is_active, created_by, version, category,
    transit_time_weight, processing_time_weight, inventory_weight, express_weight,
    split_penalty_base, split_penalty_exponent, split_penalty_multiplier,
    high_value_threshold, high_value_penalty, same_day_penalty, next_day_penalty,
    distance_weight, distance_threshold, base_confidence, peak_season_adjustment,
    weather_adjustment, hazmat_adjustment, execution_priority, cache_ttl_minutes
) VALUES (
    'ELECTRONICS_PREMIUM_SCORING', 'Electronics Premium Scoring', 
    'Enhanced scoring for high-value electronics with stricter security requirements', 
    true, 'security_team', '1.0', 'ELECTRONICS',
    -15.0, -8.0, 60.0, 30.0,
    20.0, 1.8, 12.0,
    1000.0, 30.0, 35.0, 25.0,
    -0.8, 75.0, 0.85, -0.15,
    -0.08, -0.20, 2, 30
);

-- Express Delivery Scoring Configuration
INSERT INTO scoring_configuration (
    id, name, description, is_active, created_by, version, category,
    transit_time_weight, processing_time_weight, inventory_weight, express_weight,
    split_penalty_base, split_penalty_exponent, split_penalty_multiplier,
    high_value_threshold, high_value_penalty, same_day_penalty, next_day_penalty,
    distance_weight, distance_threshold, base_confidence, peak_season_adjustment,
    weather_adjustment, hazmat_adjustment, execution_priority, cache_ttl_minutes
) VALUES (
    'EXPRESS_DELIVERY_SCORING', 'Express Delivery Scoring', 
    'Optimized scoring for same-day and next-day delivery requirements', 
    true, 'logistics_team', '1.0', 'EXPRESS',
    -20.0, -10.0, 40.0, 50.0,
    30.0, 2.0, 15.0,
    300.0, 15.0, 40.0, 30.0,
    -1.0, 50.0, 0.75, -0.20,
    -0.10, -0.25, 3, 15
);

-- Hazmat Scoring Configuration  
INSERT INTO scoring_configuration (
    id, name, description, is_active, created_by, version, category,
    transit_time_weight, processing_time_weight, inventory_weight, express_weight,
    split_penalty_base, split_penalty_exponent, split_penalty_multiplier,
    high_value_threshold, high_value_penalty, same_day_penalty, next_day_penalty,
    distance_weight, distance_threshold, base_confidence, peak_season_adjustment,
    weather_adjustment, hazmat_adjustment, execution_priority, cache_ttl_minutes,
    custom_scoring_script
) VALUES (
    'HAZMAT_SCORING', 'Hazmat Material Scoring', 
    'Specialized scoring for hazardous materials with compliance requirements', 
    true, 'compliance_team', '1.0', 'HAZMAT',
    -12.0, -7.0, 45.0, 25.0,
    25.0, 1.6, 12.0,
    200.0, 25.0, 50.0, 40.0,
    -0.6, 80.0, 0.70, -0.12,
    -0.15, -0.30, 4, 45,
    'base = location.transitTime * scoring.transitTimeWeight + inventory.ratio * scoring.inventoryWeight; return order.isHazmat ? base * 0.8 : base'
);