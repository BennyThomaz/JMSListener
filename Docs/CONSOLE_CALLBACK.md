# Console Callback Configuration

## Overview

The Console Message Callback can now be enabled or disabled through configuration, providing flexibility in how messages are processed and logged.

## Configuration Property

```properties
# Console Callback Configuration
console.callback.enabled=true   # Enable console output (default: true)
console.callback.enabled=false  # Disable console output
```

## Behavior

### When Enabled (`console.callback.enabled=true`)
- All received JMS messages are printed to the console
- Detailed message information is displayed including:
  - Message ID
  - JMS Timestamp
  - Correlation ID
  - Message content (text, bytes, map, object, stream)
  - Destination information

### When Disabled (`console.callback.enabled=false`)
- No console output for received messages
- Only application logs (startup, errors, etc.) are shown
- Reduces console clutter in production environments

## Configuration Files

### Default Configuration (`application.properties`)
```properties
console.callback.enabled=true
```
- Console callback **enabled** by default for development/debugging

### HTTP Configuration (`application-http.properties`)
```properties
console.callback.enabled=false
```
- Console callback **disabled** when HTTP callback is primary
- Reduces noise when messages are being sent to HTTP endpoints

### Transacted Configuration (`application-transacted.properties`)
```properties
console.callback.enabled=true
```
- Console callback **enabled** for transaction monitoring
- Helpful for debugging transaction rollback scenarios

## Safety Features

### Fallback Protection
If **no callbacks are configured** (both console and HTTP disabled), the application automatically enables the console callback as a fallback:

```
WARN  - No callbacks configured! Adding console callback as fallback
INFO  - Added console message callback
```

This prevents messages from being silently ignored.

### Startup Logging
The application logs which callbacks are enabled during startup:

```
INFO  - Added console message callback
INFO  - HTTP callback disabled in configuration
```

or

```
INFO  - Console callback disabled in configuration  
INFO  - Added HTTP message callback for URL: http://localhost:8080/api/jms-messages
```

## Use Cases

### Development/Testing
```properties
console.callback.enabled=true
http.callback.enabled=false
```
- See all messages in console for debugging
- No external HTTP calls

### Production with HTTP Integration
```properties
console.callback.enabled=false
http.callback.enabled=true
```
- Clean console output
- Messages sent to HTTP endpoint only

### Full Logging (Development)
```properties
console.callback.enabled=true
http.callback.enabled=true
```
- Messages displayed in console AND sent to HTTP endpoint
- Maximum visibility for debugging

### Troubleshooting Mode
```properties
console.callback.enabled=true
http.callback.enabled=true
http.callback.url=http://localhost:8080/api/debug
```
- Dual output for comparing console vs HTTP processing
- Useful for debugging HTTP callback issues

## Example Output

### Console Enabled
```
=== JMS Message Received ===
Message ID: ID:192.168.0.196-51616-1625097168-1:1:1:1:1
JMS Timestamp: Thu Jul 01 10:32:48 GMT 2021
Content: Hello World from JMS Queue
Destination: jms/MyQueue
============================
```

### Console Disabled
```
(No message output - only application logs)
```

This configuration provides fine-grained control over message output, making the application suitable for both development and production environments.
