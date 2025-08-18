# JMS Listener Application

A production-ready Java application that listens on a JMS queue on WebLogic Server with robust error handling, logging, and configuration management.

## Features

- **Production-Ready**: Comprehensive error handling and graceful shutdown
- **Transaction Support**: Automatic transaction rollback on HTTP callback failures
- **Configurable**: External configuration file support with multiple profiles
- **Logging**: Structured logging with file rotation and transaction monitoring
- **Callback-Based**: Pluggable message processing via callback interface
- **HTTP Integration**: Built-in HTTP callback using Java's native HTTP client
- **Connection Management**: Proper resource cleanup and connection monitoring
- **Monitoring**: Built-in heartbeat, health monitoring, and poison message detection
- **Fault Tolerance**: Automatic retry logic and message redelivery support

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/jms/
│   │       ├── JMSListenerApplication.java    # Main application class
│   │       ├── callback/
│   │       │   ├── MessageCallback.java       # Callback interface
│   │       │   ├── ConsoleMessageCallback.java # Console implementation
│   │       │   ├── HttpMessageCallback.java   # HTTP callback implementation
│   │       │   └── CompositeMessageCallback.java # Multiple callback support
│   │       ├── config/
│   │       │   └── JMSConfig.java             # Configuration manager
│   │       └── listener/
│   │           └── JMSListener.java           # Main JMS listener class
│   └── resources/
│       ├── application.properties             # Default configuration
│       ├── application-http.properties        # HTTP callback configuration
│       ├── application-transacted.properties  # Transacted mode configuration
│       └── logback.xml                       # Logging configuration
├── target/                                   # Build output
├── logs/                                     # Application logs
├── pom.xml                                   # Maven configuration
├── build.bat                                 # Windows build script
├── run-transacted.bat                        # Run with transaction support
├── run-test-server.bat                       # Start transaction test server
├── transaction-test-server.py                # HTTP failure simulation server
├── TRANSACTION_ROLLBACK.md                   # Transaction rollback documentation
└── README.md                                 # This file
```

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- WebLogic Server with configured JMS resources
- Access to WebLogic JMS queue and connection factory
- **WebLogic Client JAR**: Place `wlclient.jar` in the `lib/` directory (see `lib/README.md` for details)

## Configuration

Edit `src/main/resources/application.properties` to configure your environment:

### JMS Configuration
```properties
# JNDI names for JMS resources
jms.connection.factory.jndi=weblogic.jms.ConnectionFactory
jms.queue.jndi=jms/MyQueue

# WebLogic connection details
jms.initial.context.factory=weblogic.jndi.WLInitialContextFactory
jms.provider.url=t3://localhost:7001
jms.security.principal=weblogic
jms.security.credentials=password
```

### Message Processing
```properties
# Session configuration
jms.message.acknowledgment.mode=AUTO_ACKNOWLEDGE
jms.session.transacted=false

# Optional message selector
jms.consumer.message.selector=

# Consumer options
jms.consumer.no.local=false

# JNDI timeout settings (in milliseconds)
jms.jndi.timeout=60000
jms.jndi.read.timeout=120000
```

### Application Settings
```properties
# Shutdown timeout in milliseconds
app.shutdown.timeout=30000

# Heartbeat interval in milliseconds
app.heartbeat.interval=60000

# Retry configuration
app.max.retry.attempts=3
app.retry.delay=5000
```

## Building the Application

**Important**: Before building, ensure you have placed `wlclient.jar` in the `lib/` directory. See `lib/README.md` for instructions.

1. **Install Dependencies**:
   ```bash
   mvn clean install
   ```

2. **Build JAR with Dependencies**:
   ```bash
   mvn package
   ```

   This creates `target/jms-listener-1.0.0-jar-with-dependencies.jar`

### Building Without WebLogic Client (Development Mode)

If you don't have `wlclient.jar` available, you can temporarily comment out the WebLogic dependency in `pom.xml`:

```xml
<!-- Comment out this dependency for development -->
<!--
<dependency>
    <groupId>com.oracle.weblogic</groupId>
    <artifactId>wlclient</artifactId>
    <version>14.1.1</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/wlclient.jar</systemPath>
</dependency>
-->

## Running the Application

### Option 1: Using Startup Scripts

**Windows**:
```cmd
start.bat
```

**Unix/Linux**:
```bash
chmod +x start.sh
./start.sh
```

### Option 2: Using Maven

```bash
mvn exec:java -Dexec.mainClass="com.example.jms.JMSListenerApplication"
```

### Option 3: Direct Java Execution

```bash
java -cp target/jms-listener-1.0.0-jar-with-dependencies.jar com.example.jms.JMSListenerApplication
```

### Option 4: Custom Configuration File

```bash
java -cp target/jms-listener-1.0.0-jar-with-dependencies.jar com.example.jms.JMSListenerApplication /path/to/custom.properties
```

## Logging

The application uses Logback for logging with the following features:

- **Console Output**: Real-time log messages
- **File Logging**: Rotating log files in `logs/` directory
- **Error Logging**: Separate error log file
- **Configurable Levels**: Debug, Info, Warn, Error

Log files:
- `logs/jms-listener.log` - Main application log
- `logs/jms-listener-error.log` - Error-only log

## Transaction Rollback Support

The application provides robust **transaction rollback** capabilities for JMS sessions when HTTP callbacks fail:

### Transacted Mode Configuration

```properties
# Enable transaction support
jms.session.transacted=true
jms.message.acknowledgment.mode=SESSION_TRANSACTED

# HTTP callback with rollback support
http.callback.enabled=true
http.callback.url=http://your-api-endpoint/messages
```

### How Transaction Rollback Works

1. **Message Received**: JMS transaction begins automatically
2. **HTTP Callback**: Message is sent to configured HTTP endpoint
3. **Success**: Transaction commits, message is removed from queue
4. **Failure**: Transaction rolls back, message returns to queue for redelivery

### Benefits

- **Message Integrity**: No message loss if HTTP endpoints are unavailable
- **Automatic Recovery**: Messages are redelivered when systems recover
- **Consistency**: Atomic operations between JMS consumption and HTTP delivery

### Running with Transaction Support

```bash
# Use the transacted configuration
run-transacted.bat

# Or specify the configuration manually
java -jar target/jms-listener-1.0-SNAPSHOT.jar --spring.config.name=application-transacted
```

### Testing Transaction Rollback

Use the provided test server to simulate HTTP failures:

```bash
# Start test server with 30% failure rate
run-test-server.bat

# Or run directly with Python
python transaction-test-server.py --demo
```

For detailed information, see: **[TRANSACTION_ROLLBACK.md](Docs\TRANSACTION_ROLLBACK.md)**

## Message Processing

The application uses a callback-based architecture for message processing:

1. **MessageCallback Interface**: Defines the contract for message processing
2. **ConsoleMessageCallback**: Default implementation that prints messages to console
3. **Custom Callbacks**: Implement `MessageCallback` interface for custom processing

### Custom Message Processing Example

```java
public class CustomMessageCallback implements MessageCallback {
    @Override
    public void onMessage(Message message) throws JMSException {
        // Your custom message processing logic here
        if (message instanceof TextMessage) {
            String text = ((TextMessage) message).getText();
            // Process the text message
        }
    }
}
```

## Monitoring and Health Checks

The application includes built-in monitoring features:

- **Heartbeat Thread**: Periodic status logging
- **Connection Monitoring**: JMS connection exception handling
- **Graceful Shutdown**: Proper resource cleanup on application termination

## Error Handling

The application implements comprehensive error handling:

- **Connection Failures**: Automatic detection and logging
- **Message Processing Errors**: Isolated error handling per message
- **Transaction Support**: Rollback on processing failures (if configured)
- **Retry Logic**: Configurable retry attempts for failed operations

## Deployment Considerations

### Production Deployment

1. **Environment-Specific Configuration**: Use different property files for different environments
2. **JVM Tuning**: Adjust heap size based on message volume
3. **Log Management**: Configure log rotation and archival
4. **Monitoring Integration**: Integrate with your monitoring solution
5. **Security**: Use secure credentials and encrypted connections

### WebLogic Setup Requirements

Ensure the following resources are configured in WebLogic:

1. **JMS Server**: Active JMS server
2. **JMS Module**: Deployed JMS module with queue
3. **Connection Factory**: Configured connection factory
4. **Security**: Proper user permissions for JMS resources

### Example WebLogic WLST Script

```python
# Create JMS resources (example)
connect('weblogic', 'password', 't3://localhost:7001')
edit()
startEdit()

# Create JMS Server
cd('/JMSServers')
cmo.createJMSServer('MyJMSServer')
cd('/JMSServers/MyJMSServer')
cmo.setTargets(jarray.array([ObjectName('com.bea:Name=AdminServer,Type=Server')], ObjectName))

# Create JMS Module
cd('/')
cmo.createJMSSystemResource('MyJMSModule')
cd('/JMSSystemResources/MyJMSModule')
cmo.setTargets(jarray.array([ObjectName('com.bea:Name=AdminServer,Type=Server')], ObjectName))

# Create Queue
cd('/JMSSystemResources/MyJMSModule/JMSResource/MyJMSModule')
cmo.createQueue('MyQueue')
cd('/JMSSystemResources/MyJMSModule/JMSResource/MyJMSModule/Queues/MyQueue')
cmo.setJNDIName('jms/MyQueue')
cmo.setSubDeploymentName('MyJMSServer')

save()
activate()
```

## Troubleshooting

### Common Issues

1. **Connection Refused**: Check WebLogic server status and URL
2. **Authentication Failed**: Verify username/password
3. **JNDI Lookup Failed**: Ensure JMS resources are deployed
4. **ClassNotFound**: Check WebLogic libraries in classpath

### Debug Mode

Enable debug logging by setting in `application.properties`:
```properties
logging.level.com.example.jms=DEBUG
```

## License

This project is licensed under the MIT License - see below for details:

```
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Support

For issues and questions, please check the logs and configuration settings first. The application provides detailed logging to help diagnose problems.

## JNDI Timeout Configuration

For reliable connectivity to WebLogic servers, especially remote ones:

```properties
# JNDI connection timeout (default: 60 seconds)
jms.jndi.timeout=60000

# JNDI read timeout for lookup operations (default: 120 seconds)
jms.jndi.read.timeout=120000
```

**Benefits**:
- Prevents connection hangs with slow networks
- Configurable timeouts for different environments
- Better error handling for network issues

For detailed configuration guidance, see: **[Docs\JNDI_TIMEOUT.md](Docs\JNDI_TIMEOUT.md)**
