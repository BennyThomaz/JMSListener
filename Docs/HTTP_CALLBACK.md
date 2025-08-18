# HTTP Message Callback

The `HttpMessageCallback` class allows JMS messages to be forwarded to HTTP endpoints in JSON format. This implementation uses Java's built-in `java.net.http.HttpClient` (available since Java 11) for reliable and efficient HTTP communication.

## Features

- **Native Java HTTP Client**: Uses `java.net.http.HttpClient` - no external dependencies required
- **JSON Conversion**: Converts JMS messages to structured JSON format
- **HTTP Methods**: Supports POST and PUT methods
- **Retry Logic**: Configurable retry attempts with delay
- **Timeout Configuration**: Connection and socket timeout settings
- **Custom Headers**: Support for authorization and custom headers
- **Multiple Message Types**: Handles Text, Bytes, Map, Object, and Stream messages
- **Error Handling**: Comprehensive error handling and logging

## Configuration

Enable HTTP callback in `application.properties`:

```properties
# Enable HTTP callback
http.callback.enabled=true
http.callback.url=http://localhost:8080/api/jms-messages

# HTTP method (POST or PUT)
http.callback.method=POST

# Content type
http.callback.content.type=application/json

# Timeout settings (milliseconds)
http.callback.timeout.connection=5000
http.callback.timeout.socket=10000

# Retry configuration
http.callback.retry.attempts=3
http.callback.retry.delay=1000

# Custom headers
http.callback.headers.authorization=Bearer your-api-token
http.callback.headers.x-source=JMS-Listener
```

## JSON Message Format

The HTTP callback sends JMS messages in the following JSON format:

```json
{
  "messageId": "ID:414d512043484152544552533120202016f8e6d20520f30",
  "timestamp": 1690750800000,
  "messageType": "TextMessage",
  "jmsTimestamp": 1690750800000,
  "correlationId": "CORR-123",
  "destination": "queue:///jms/MyQueue",
  "replyTo": null,
  "deliveryMode": 2,
  "priority": 4,
  "expiration": 0,
  "redelivered": false,
  "properties": {
    "userId": "john.doe",
    "priority": "high"
  },
  "contentType": "text",
  "content": "Hello, this is a test message"
}
```

### Content Types

Different JMS message types are handled as follows:

- **TextMessage**: `contentType: "text"`, content as string
- **BytesMessage**: `contentType: "bytes"`, content as Base64 encoded string (limited to 1MB)
- **MapMessage**: `contentType: "map"`, content as JSON object
- **ObjectMessage**: `contentType: "object"`, content as string representation
- **StreamMessage**: `contentType: "stream"`, placeholder content

## HTTP Headers

The following headers are automatically sent with each request:

- `Content-Type`: As configured (default: `application/json`)
- `X-Timestamp`: Current timestamp in milliseconds
- `Authorization`: If configured
- `X-Source`: If configured
- Custom headers as configured with `http.callback.headers.*`

## Testing

### 1. Start Test HTTP Server

A Python test server is provided:

```bash
python test-http-server.py
```

This starts a server on `http://localhost:8080` that logs received messages.

### 2. Use Test Configuration

Use the provided test configuration:

```bash
java -cp target/jms-listener-1.0.0-jar-with-dependencies.jar com.example.jms.JMSListenerApplication src/main/resources/application-http.properties
```

### 3. Send Test Message

Send a JMS message to your configured queue. The message will be:
1. Printed to console (ConsoleMessageCallback)
2. Posted to HTTP endpoint (HttpMessageCallback)

## Composite Callback Behavior

The application uses `CompositeMessageCallback` to combine multiple callbacks:

- **Console Callback**: Always active for logging/debugging
- **HTTP Callback**: Added when `http.callback.enabled=true`

By default, the composite continues execution even if one callback fails (`continueOnError=true`).

## Error Handling

### HTTP Errors

- **Connection Timeout**: Configurable timeout for establishing connections
- **Socket Timeout**: Configurable timeout for data transfer
- **HTTP Status Codes**: 2xx codes are considered successful
- **Retry Logic**: Failed requests are retried with exponential backoff

### JMS Integration

- If HTTP callback fails after all retries, a `JMSException` is thrown
- This can trigger JMS transaction rollback if sessions are transacted
- Error details are logged for debugging

## Production Considerations

### Security

1. **HTTPS**: Use HTTPS URLs for production
2. **Authentication**: Configure proper authorization headers
3. **Rate Limiting**: Consider rate limiting on the receiving endpoint

### Performance

1. **Timeout Values**: Tune timeouts based on your network conditions
2. **Retry Configuration**: Balance reliability vs. message throughput
3. **Message Size**: Large messages may impact performance

### Monitoring

1. **Logs**: Monitor application logs for HTTP errors
2. **Metrics**: Consider adding metrics for success/failure rates
3. **Alerting**: Set up alerts for repeated HTTP failures

## Example Receiving Endpoint

Here's a simple example of a receiving endpoint in Node.js:

```javascript
const express = require('express');
const app = express();

app.use(express.json());

app.post('/api/jms-messages', (req, res) => {
    console.log('Received JMS message:', {
        messageId: req.body.messageId,
        contentType: req.body.contentType,
        content: req.body.content
    });
    
    res.json({
        status: 'success',
        messageId: req.body.messageId,
        timestamp: Date.now()
    });
});

app.listen(8080, () => {
    console.log('JMS HTTP receiver listening on port 8080');
});
```

## Troubleshooting

### Common Issues

1. **Connection Refused**: Check if target server is running and URL is correct
2. **Timeout Errors**: Increase timeout values or check network connectivity
3. **Authentication Failures**: Verify authorization header configuration
4. **JSON Parsing Errors**: Check receiving endpoint JSON parsing

### Debug Mode

Enable debug logging to see detailed HTTP request/response information:

```properties
logging.level.com.example.jms.callback=DEBUG
```

This will log:
- HTTP request details
- Response status codes
- Retry attempts
- Error messages
