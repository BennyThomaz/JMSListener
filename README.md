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
│   │       ├── provider/
│   │       │   ├── JMSProviderConfig.java         # Provider interface
│   │       │   └── JsonProviderConfig.java        # JSON-based provider implementation
│   │       └── listener/
│   │           └── JMSListener.java           # Main JMS listener class
│   └── resources/
│       ├── config/
│       │   ├── weblogic.provider.json            # WebLogic provider configuration
│       │   ├── artemis.provider.json             # Artemis provider configuration  
│       │   ├── activemq.provider.json            # ActiveMQ provider configuration
│       │   └── generic.provider.json             # Generic JMS provider configuration
│       ├── application.properties             # Default configuration (WebLogic)
│       ├── application-artemis.properties     # Apache Artemis AMQ configuration
│       ├── application-http.properties        # HTTP callback configuration
│       ├── application-transacted.properties  # Transacted mode configuration
│       └── logback.xml                       # Logging configuration
├── target/                                   # Build output
├── logs/                                     # Application logs
├── pom.xml                                   # Maven configuration
├── build.bat                                 # Windows build script
├── start-artemis.bat                         # Run with Apache Artemis configuration
├── run-transacted.bat                        # Run with transaction support
├── run-test-server.bat                       # Start transaction test server
├── transaction-test-server.py                # HTTP failure simulation server
├── ARTEMIS_CONFIG.md                         # Apache Artemis configuration guide
├── JSON_PROVIDER_CONFIG.md                   # JSON-based provider configuration guide
├── TRANSACTION_ROLLBACK.md                   # Transaction rollback documentation
├── WEBLOGIC_CONFIG.md                        # WebLogic-specific configuration guide
└── README.md                                 # This file
```

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- **JMS Provider**: One of the following:
  - **WebLogic Server** with configured JMS resources
  - **Apache Artemis AMQ** broker
- Access to JMS queue and connection factory
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

# Idle Reconnection
# Interval in milliseconds to reconnect if no messages received (default: 30 minutes)
# Set to 0 to disable
jms.idle.reconnect.interval=1800000
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

## JMS Provider Configuration

The application supports multiple JMS providers through individual JSON configuration files. Each provider has its own configuration file that contains JNDI environment properties specific to that provider.

### Configuration File Selection

Specify which provider configuration to use in your `application.properties`:

```properties
# Provider configuration file (in config/ directory)
jms.provider.config.file=weblogic.provider.json
```

### Available Provider Files

- **weblogic.provider.json** - WebLogic Server optimizations
- **artemis.provider.json** - Apache Artemis AMQ optimizations  
- **activemq.provider.json** - Apache ActiveMQ Classic optimizations
- **generic.provider.json** - Generic JMS provider (minimal configuration)

### Provider File Format

Each provider file contains a simple "properties" object with key-value pairs:

```json
{
  "properties": {
    "weblogic.jndi.connectTimeout": "15000",
    "weblogic.jndi.readTimeout": "30000", 
    "weblogic.jndi.connectionRetryCount": "3",
    "weblogic.jndi.tcpNoDelay": "true"
  }
}
```

### WebLogic Server Configuration
```properties
# In application.properties
jms.provider.config.file=weblogic.provider.json
jms.initial.context.factory=weblogic.jndi.WLInitialContextFactory
jms.provider.url=t3://localhost:7001
```

### Apache Artemis AMQ Configuration
```properties
# In application-artemis.properties  
jms.provider.config.file=artemis.provider.json
jms.initial.context.factory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory
jms.provider.url=tcp://localhost:61616
```

Use the appropriate configuration file:
- **WebLogic**: Set `jms.provider.config.file=weblogic.provider.json`  
- **Artemis**: Set `jms.provider.config.file=artemis.provider.json`
- **ActiveMQ**: Set `jms.provider.config.file=activemq.provider.json`
- **Other**: Set `jms.provider.config.file=generic.provider.json`

For detailed Artemis configuration, see: **[ARTEMIS_CONFIG.md](ARTEMIS_CONFIG.md)**

## Building the Application

### Dependencies

**For WebLogic**: Place `wlclient.jar` in the `lib/` directory. See `lib/README.md` for instructions.

**For Artemis**: Add Artemis client dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.apache.activemq</groupId>
    <artifactId>artemis-jms-client</artifactId>
    <version>2.32.0</version>
</dependency>
```

### Build Steps

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

## Provider Architecture

The application uses a **simplified JSON-based provider configuration system** that loads provider-specific JNDI properties from individual JSON files:

- **WebLogic Server**: `weblogic.provider.json` with WebLogic-specific optimizations
- **Apache Artemis**: `artemis.provider.json` with Artemis-specific optimizations  
- **Apache ActiveMQ**: `activemq.provider.json` with ActiveMQ-specific optimizations
- **Generic JMS**: `generic.provider.json` for other JMS providers
- **Extensible**: Easy to add new providers by creating new JSON files

### Configuration-Driven Approach

Provider configurations are defined in individual JSON files in `src/main/resources/config/`:

**weblogic.provider.json**:
```json
{
  "properties": {
    "weblogic.jndi.connectTimeout": "15000",
    "weblogic.jndi.readTimeout": "30000",
    "weblogic.jndi.connectionRetryCount": "3",
    "weblogic.jndi.tcpNoDelay": "true",
    "weblogic.jndi.WLContext.ENABLE_SERVER_AFFINITY": "false"
  }
}
```

**artemis.provider.json**:
```json
{
  "properties": {
    "connection.ttl": "15000",
    "connection.retry.interval": "5000", 
    "consumer.window.size": "1048576",
    "producer.window.size": "65536",
    "connection.compression.enabled": "false"
  }
}
```

### How It Works

1. **Configuration**: Set `jms.provider.config.file=weblogic.provider.json` in application.properties
2. **Loading**: `JsonProviderConfig` loads the specified JSON file
3. **Application**: All properties from the JSON file are applied directly to the JNDI environment
4. **No Code Changes**: Add new providers by creating new JSON files

### Benefits

- **No Code Changes**: Add new providers without Java modifications
- **Maintainable**: All provider logic in simple JSON configuration files
- **Flexible**: Easy to override properties per environment
- **Testable**: Easy to validate and test configurations
- **Simple**: Direct property mapping from JSON to JNDI environment

Provider selection is explicit via the `jms.provider.config.file` property.

For detailed information, see: **[JSON_PROVIDER_CONFIG.md](JSON_PROVIDER_CONFIG.md)**

### Adding New Providers

To add support for a new JMS provider:

1. **Create JSON file**: Add a new `.provider.json` file in `src/main/resources/config/`
2. **Define properties**: Add provider-specific JNDI properties to the "properties" object
3. **Configure**: Set `jms.provider.config.file=yourprovider.provider.json` in application.properties

Example new provider file (`myprovider.provider.json`):
```json
{
  "properties": {
    "com.myprovider.timeout": "30000",
    "com.myprovider.retries": "3",
    "com.myprovider.poolsize": "10"
  }
}
```
