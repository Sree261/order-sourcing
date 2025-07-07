-- H2 Compatible Test Data
-- Insert some sample locations for testing
INSERT INTO location (id, name, latitude, longitude, transit_time) VALUES
(1, 'Downtown Warehouse', 40.7128, -74.0060, 1),
(2, 'Suburb Store', 40.7489, -73.9857, 2),
(3, 'Airport Distribution Center', 40.6892, -74.1745, 1);

-- Insert sample inventory data
INSERT INTO inventory (location_id, sku, quantity, processing_time) VALUES
(1, 'PHONE123', 50, 1),
(1, 'LAPTOP456', 25, 1),
(1, 'TABLET789', 30, 1),
(2, 'PHONE123', 20, 2),
(2, 'HEADPHONES101', 100, 1),
(3, 'LAPTOP456', 15, 1),
(3, 'TABLET789', 40, 1);

-- Insert basic location filters
INSERT INTO location_filter (id, name, description, filter_script, is_active, created_by, version, category, execution_priority, cache_ttl_minutes) VALUES 
('STANDARD_DELIVERY_RULE', 'Standard Delivery Filter', 'Basic filter for standard delivery items',
'// Standard delivery - less restrictive
return true;', 
true, 'system', '1.0', 'DELIVERY_TYPE', 3, 120);

-- Insert sample carrier configurations
INSERT INTO carrier_configuration (carrier_code, service_level, delivery_type, base_transit_days, max_transit_days, transit_time_multiplier, pickup_cutoff_time, next_pickup_time, delivery_start_time, delivery_end_time, weekend_pickup, weekend_delivery, holiday_service, residential_delivery, signature_required, max_distance_km, base_cost, carrier_priority, supports_hazmat, supports_cold_chain, supports_high_value, max_value_limit, on_time_performance, average_delay_hours, is_peak_season_service, peak_season_delay_days) VALUES
('UPS', 'GROUND', 'STANDARD', 3, 5, 1.0, '17:00', '09:00', '09:00', '18:00', false, false, false, true, false, 1000, 15.99, 1, false, false, true, 5000, 0.95, 4, true, 1),
('FEDEX', 'GROUND', 'STANDARD', 3, 5, 1.0, '17:00', '09:00', '09:00', '18:00', false, false, false, true, false, 1000, 16.99, 2, false, false, true, 5000, 0.94, 6, true, 1);

-- Insert test scoring configurations
INSERT INTO scoring_configuration (
    id, name, description, is_active, created_by, version, category, 
    transit_time_weight, processing_time_weight, inventory_weight, express_weight,
    split_penalty_base, split_penalty_exponent, split_penalty_multiplier,
    high_value_threshold, high_value_penalty, same_day_penalty, next_day_penalty,
    distance_weight, distance_threshold, base_confidence, peak_season_adjustment,
    weather_adjustment, hazmat_adjustment, execution_priority, cache_ttl_minutes
) VALUES (
    'DEFAULT_SCORING', 'Default Test Scoring Configuration', 
    'Standard scoring weights for test scenarios', 
    true, 'test_system', '1.0', 'DEFAULT',
    -10.0, -5.0, 50.0, 20.0,
    15.0, 1.5, 10.0,
    500.0, 20.0, 25.0, 15.0,
    -0.5, 100.0, 0.8, -0.1,
    -0.05, -0.15, 1, 60
);

-- Test scoring configuration for electronics
INSERT INTO scoring_configuration (
    id, name, description, is_active, created_by, version, category,
    transit_time_weight, processing_time_weight, inventory_weight, express_weight,
    split_penalty_base, split_penalty_exponent, split_penalty_multiplier,
    high_value_threshold, high_value_penalty, same_day_penalty, next_day_penalty,
    distance_weight, distance_threshold, base_confidence, peak_season_adjustment,
    weather_adjustment, hazmat_adjustment, execution_priority, cache_ttl_minutes
) VALUES (
    'TEST_ELECTRONICS_SCORING', 'Test Electronics Scoring', 
    'Test scoring configuration for electronics items', 
    true, 'test_team', '1.0', 'ELECTRONICS',
    -12.0, -6.0, 55.0, 25.0,
    18.0, 1.6, 11.0,
    750.0, 25.0, 30.0, 20.0,
    -0.6, 90.0, 0.82, -0.12,
    -0.06, -0.18, 2, 45
);