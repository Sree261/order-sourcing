# Order Sourcing Engine - Postman Collection

This directory contains a comprehensive Postman collection for testing the Order Sourcing Engine API. The collection includes all scenarios from the API examples documentation, organized into logical folders with proper test scripts and environment variables.

## Files Included

1. **`Order_Sourcing_Engine_API.postman_collection.json`** - Main collection file
2. **`Order_Sourcing_Engine.postman_environment.json`** - Environment variables
3. **`POSTMAN_COLLECTION_README.md`** - This documentation file

## Quick Start

### 1. Import into Postman

1. Open Postman
2. Click **Import** button
3. Select **Upload Files**
4. Import both files:
   - `Order_Sourcing_Engine_API.postman_collection.json`
   - `Order_Sourcing_Engine.postman_environment.json`

### 2. Set Environment

1. In Postman, select the **"Order Sourcing Engine - Local"** environment
2. Verify the `baseUrl` is set to `http://localhost:8081`

### 3. Start the Application

Ensure the Order Sourcing Engine is running locally on port 8081:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

### 4. Run Health Check

Execute the **"Service Health Check"** request in the **"06 - Health Check"** folder to verify the service is running.

## Collection Structure

### 📁 01 - Basic Scenarios
- **PDP Single Product** - Simple single item requests
- **Mixed Shopping Cart** - Multiple items with different delivery types

### 📁 02 - Business Use Cases  
- **B2B Bulk Order** - Enterprise large quantity orders
- **Electronics High-Value Order** - Premium electronics with security
- **Peak Season Order** - Holiday season with capacity constraints

### 📁 03 - Same-Day Delivery
- **Same-Day Rush Order** - Urgent same-day delivery
- **Same-Day Optimized** - Nearest location priority configuration  
- **Same-Day Multi-Item** - Multiple items same-day delivery

### 📁 04 - Performance Testing
- **Large Performance Test Order** - Stress testing with many items
- **Multi-Location Split Required** - Large quantities requiring splits

### 📁 05 - Error Scenarios
- **Missing Required Fields** - Validation error testing
- **Invalid Coordinates** - Data validation testing

### 📁 06 - Health Check
- **Service Health Check** - Service availability verification

## Features

### 🔄 Dynamic Variables
- **Unique Order IDs**: Each request generates a unique `tempOrderId` using timestamps
- **Environment Variables**: Easy switching between different environments
- **Base URL Configuration**: Centralized URL management

### 🧪 Automated Testing
- **Response Time Validation**: Ensures performance targets are met
- **Status Code Checks**: Validates expected HTTP responses  
- **Response Structure Validation**: Ensures API contract compliance
- **Business Logic Tests**: Validates specific business requirements

### 📊 Performance Monitoring
- **Response Time Tracking**: Built-in performance monitoring
- **Expected Time Assertions**: Validates sub-50ms response times
- **Batch vs Sequential**: Different expectations for different processing strategies

## Expected Response Times

| Scenario | Expected Time | Processing Strategy |
|----------|---------------|-------------------|
| PDP Request | 15-40ms | Sequential |
| Mixed Cart | 40-80ms | Batch |
| B2B Order | 80-150ms | Batch |
| Same-Day Rush | 15-30ms | Sequential |
| Performance Test | 150-300ms | Batch |

## Environment Variables

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `baseUrl` | `http://localhost:8081` | API base URL |
| `apiPath` | `/api/sourcing/source` | Main endpoint path |
| `healthPath` | `/actuator/health` | Health check path |
| `timestamp` | *auto-generated* | Unique timestamp for order IDs |

## Test Scripts

### Pre-request Scripts
- **Dynamic Timestamp Generation**: Creates unique order IDs
- **Environment Setup**: Ensures required variables are set

### Test Scripts
- **Response Validation**: Status codes, response times, structure
- **Business Logic Validation**: Same-day delivery constraints, scoring
- **Performance Assertions**: Response time thresholds
- **Error Handling**: Proper error response validation

## Usage Examples

### 1. Single Request Testing
1. Select any request from the collection
2. Review the request body and parameters
3. Click **Send**
4. Review the response and test results

### 2. Folder-Level Testing  
1. Right-click on any folder (e.g., "01 - Basic Scenarios")
2. Select **Run Collection**
3. Review the test runner results

### 3. Full Collection Testing
1. Click the collection name
2. Click **Run**
3. Select all requests or specific folders
4. Monitor the complete test execution

### 4. Environment Switching
1. Duplicate the environment for different setups (dev, staging, prod)
2. Update the `baseUrl` for each environment
3. Switch environments as needed

## Database Configuration

Some requests require specific database configurations. Ensure you have the following data:

### Location Filters
- `STANDARD_DELIVERY_RULE`
- `SDD_FILTER_RULE` 
- `ELECTRONICS_SECURE_RULE`
- `SAME_DAY_DELIVERY_FILTER`
- `PEAK_SEASON_RULE`

### Scoring Configurations  
- `EXPRESS_DELIVERY_SCORING`
- `ELECTRONICS_PREMIUM_SCORING`
- `SAME_DAY_DELIVERY_SCORING`
- `PEAK_SEASON_SCORING`
- `B2B_BULK_SCORING`

See `API_EXAMPLES.md` for the SQL scripts to create these configurations.

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Ensure the application is running on port 8081
   - Check the `baseUrl` environment variable

2. **404 Not Found**
   - Verify the API endpoint paths are correct
   - Ensure you're using the correct Spring profile

3. **400 Bad Request**
   - Check that required fields are present
   - Verify the locationFilterId values exist in the database

4. **Slow Response Times**
   - Monitor database performance
   - Check for proper indexing
   - Verify cache configuration

### Debug Tips

1. **Enable Console Logging**: Check the Postman console for detailed request/response logs
2. **Response Inspection**: Use the Postman response viewer to inspect JSON structure
3. **Environment Variables**: Verify all variables are set correctly in the active environment
4. **Database State**: Ensure test data is properly loaded

## Contributing

When adding new requests to the collection:

1. **Follow Naming Convention**: Use descriptive names that match the use case
2. **Add Proper Tests**: Include response validation and performance assertions
3. **Document Expected Behavior**: Add descriptions explaining the request purpose
4. **Use Environment Variables**: Leverage existing variables for consistency
5. **Include Error Scenarios**: Test both success and failure cases

## Support

For issues or questions:
1. Check the main `API_EXAMPLES.md` documentation
2. Review the application logs for detailed error information
3. Verify database configuration and test data
4. Ensure proper Spring profile configuration

This Postman collection provides comprehensive testing coverage for the Order Sourcing Engine API, enabling efficient development, testing, and demonstration of the system's capabilities.