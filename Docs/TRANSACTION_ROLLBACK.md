# JMS Transaction Rollback for HTTP Callback Failures

## Overview

This JMS Listener application provides robust transaction support that automatically rolls back JMS transactions when HTTP callback endpoints fail. This ensures message integrity and prevents data loss in distributed systems.

## How Transaction Rollback Works

### 1. Transacted Session Configuration

To enable transaction rollback, configure your application with transacted JMS sessions:

```properties
# Enable transacted sessions
jms.session.transacted=true
jms.message.acknowledgment.mode=SESSION_TRANSACTED
```

### 2. Transaction Flow

When a message is received:

1. **Transaction Start**: A JMS transaction begins automatically
2. **Message Processing**: The callback processes the message
3. **HTTP Request**: If HTTP callback is enabled, an HTTP request is sent
4. **Success Path**: If HTTP request succeeds → JMS transaction commits
5. **Failure Path**: If HTTP request fails → JMS transaction rolls back

### 3. HTTP Failure Detection

The HTTP callback automatically detects failures and triggers rollback:

- **Connection timeouts** (configurable via `http.callback.timeout.connection`)
- **Socket timeouts** (configurable via `http.callback.timeout.socket`) 
- **HTTP error responses** (non-2xx status codes)
- **Network exceptions** (connection refused, DNS failures, etc.)

### 4. Retry Logic Before Rollback

Before triggering a rollback, the system attempts retries:

```properties
# Retry configuration
http.callback.retry.attempts=5
http.callback.retry.delay=2000
```

Only after all retries fail will the transaction be rolled back.

## Configuration Examples

### Transacted Configuration (application-transacted.properties)

```properties
# JMS Transaction Settings
jms.session.transacted=true
jms.message.acknowledgment.mode=SESSION_TRANSACTED

# HTTP Callback with Aggressive Retries
http.callback.enabled=true
http.callback.url=http://localhost:8080/api/jms-messages
http.callback.retry.attempts=5
http.callback.retry.delay=2000
http.callback.timeout.socket=15000
```

### Non-Transacted Configuration (application.properties)

```properties
# JMS Settings without Transactions
jms.session.transacted=false
jms.message.acknowledgment.mode=AUTO_ACKNOWLEDGE

# HTTP failures will be logged but won't affect message acknowledgment
http.callback.enabled=true
```

## Benefits of Transaction Rollback

### 1. Message Integrity
- Messages are not lost if HTTP endpoints are temporarily unavailable
- Messages are automatically redelivered for processing when systems recover

### 2. Consistency
- Ensures that message consumption and HTTP delivery are atomic operations
- Prevents partial processing scenarios

### 3. Reliability
- Handles network partitions and temporary service outages gracefully
- Provides automatic recovery without manual intervention

## Monitoring and Logging

### Transaction Events

The application logs detailed transaction information:

```
INFO  - Transaction committed successfully for message: ID:123 (processing time: 245ms)
WARN  - Transaction rolled back due to message processing error for message: ID:124 (processing time: 5230ms)
ERROR - HTTP callback failure triggered transaction rollback for message: ID:124 - message will be redelivered
```

### Poison Message Detection

The system monitors for repeated failures:

```
WARN  - Message ID:125 has been redelivered 4 times - potential poison message
```

### Key Log Levels

- `DEBUG`: Detailed transaction start/commit information
- `INFO`: Successful transaction commits with timing
- `WARN`: Transaction rollbacks and redelivery warnings
- `ERROR`: HTTP callback failures and critical transaction errors

## Deployment Scenarios

### 1. High Availability Setup

Use transacted mode with HTTP callbacks for critical systems:

```bash
java -jar jms-listener.jar --spring.config.name=application-transacted
```

### 2. Fire-and-Forget Setup

Use non-transacted mode for high-throughput, non-critical scenarios:

```bash
java -jar jms-listener.jar --spring.config.name=application
```

### 3. Development/Testing

Use the provided test HTTP server to simulate failures:

```bash
# Start test server with failure simulation
python test-http-server.py --fail-rate 0.3
```

## Best Practices

### 1. Timeout Configuration

Configure appropriate timeouts for your environment:

```properties
# Conservative settings for unreliable networks
http.callback.timeout.connection=10000
http.callback.timeout.socket=30000

# Aggressive settings for reliable, fast networks
http.callback.timeout.connection=3000
http.callback.timeout.socket=10000
```

### 2. Retry Strategy

Balance between resilience and performance:

```properties
# High resilience (slower processing)
http.callback.retry.attempts=10
http.callback.retry.delay=5000

# Fast processing (lower resilience)
http.callback.retry.attempts=3
http.callback.retry.delay=1000
```

### 3. Dead Letter Queue

For production systems, configure a Dead Letter Queue (DLQ) in WebLogic to handle poison messages that repeatedly fail processing.

### 4. Monitoring

Monitor these key metrics:
- Transaction commit/rollback ratios
- Message redelivery counts
- HTTP callback success rates
- Processing times

## Troubleshooting

### Common Issues

1. **High Rollback Rate**
   - Check HTTP endpoint availability
   - Verify network connectivity
   - Review timeout settings
   - Monitor endpoint response times

2. **Poison Messages**
   - Check for malformed message content
   - Verify message format compatibility
   - Consider implementing message validation

3. **Performance Issues**
   - Reduce retry attempts for faster processing
   - Optimize HTTP endpoint response times
   - Consider async processing patterns

### Debug Commands

```bash
# Check JMS queue depth
java -cp weblogic.jar weblogic.Admin CONNECT t3://localhost:7001 GET -type Queue -name MyQueue

# Monitor HTTP endpoint
curl -X POST http://localhost:8080/api/jms-messages -H "Content-Type: application/json" -d "{\"test\":\"message\"}"

# Tail application logs
tail -f logs/jms-listener-transacted.log
```

## Security Considerations

When using transacted mode with HTTP callbacks:

1. **Authentication**: Use secure authentication tokens
2. **Encryption**: Use HTTPS for HTTP callbacks
3. **Network Security**: Ensure proper firewall configurations
4. **Monitoring**: Log security events and failed authentication attempts

This transaction rollback feature provides a robust foundation for building reliable, fault-tolerant JMS-based distributed systems.
