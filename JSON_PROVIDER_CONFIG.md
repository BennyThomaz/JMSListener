# JSON-Based Provider Configuration

This document describes the simplified JSON-based provider configuration system that uses individual provider files with direct property mapping.

## Overview

The application uses a **simple JSON configuration** approach for JMS provider-specific settings. Each provider has its own JSON file containing JNDI properties that are applied directly to the JNDI environment. This provides:

- **Simple Configuration**: Direct property mapping from JSON to JNDI environment
- **No Code Changes**: Adding new providers requires only creating new JSON files
- **Maintainable**: Easy to modify provider settings without recompilation
- **Extensible**: Simple to add new providers and properties
- **Testable**: Provider configurations can be easily tested and validated

## Configuration Files

**Location**: `src/main/resources/config/`

### Available Provider Files

- **weblogic.provider.json** - WebLogic Server JNDI properties
- **artemis.provider.json** - Apache Artemis AMQ JNDI properties  
- **activemq.provider.json** - Apache ActiveMQ Classic JNDI properties
- **generic.provider.json** - Generic JMS provider (minimal configuration)

### File Format

Each provider file contains a simple "properties" object with key-value pairs:

```json
{
  "properties": {
    "property.name.1": "value1",
    "property.name.2": "value2",
    "property.name.3": "value3"
  }
}
```

## Provider Selection

Specify which provider configuration to use in your `application.properties`:

```properties
# Provider configuration file (in config/ directory)
jms.provider.config.file=weblogic.provider.json
```

## Supported Providers

### 1. WebLogic Server

**File**: `weblogic.provider.json`

```json
{
  "properties": {
    "weblogic.jndi.connectTimeout": "15000",
    "weblogic.jndi.readTimeout": "30000",
    "weblogic.jndi.connectionRetryCount": "3",
    "weblogic.jndi.connectionRetryInterval": "5000",
    "weblogic.jndi.tcpNoDelay": "true",
    "weblogic.jndi.WLContext.ENABLE_SERVER_AFFINITY": "false",
    "weblogic.socket.ConnectTimeout": "15000",
    "weblogic.socket.SocketTimeout": "30000"
  }
}
```

**Configuration**:
```properties
# In application.properties
jms.provider.config.file=weblogic.provider.json
jms.initial.context.factory=weblogic.jndi.WLInitialContextFactory
jms.provider.url=t3://localhost:7001
```

### 2. Apache Artemis

**File**: `artemis.provider.json`

```json
{
  "properties": {
    "connection.ttl": "15000",
    "connection.retry.interval": "5000",
    "connection.retry.interval.multiplier": "2.0",
    "connection.max.retry.interval": "30000",
    "connection.reconnect.attempts": "3",
    "consumer.window.size": "1048576",
    "consumer.max.rate": "-1",
    "producer.window.size": "65536",
    "producer.max.rate": "-1",
    "connection.compression.enabled": "false",
    "connection.confirmation.window.size": "32768"
  }
}
```

**Configuration**:
```properties
# In application-artemis.properties
jms.provider.config.file=artemis.provider.json
jms.initial.context.factory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory
jms.provider.url=tcp://localhost:61616
```

### 3. Apache ActiveMQ Classic

**File**: `activemq.provider.json`

```json
{
  "properties": {
    "java.naming.provider.url": "tcp://localhost:61616",
    "connection.timeout": "15000",
    "socket.timeout": "30000",
    "prefetchPolicy.queuePrefetch": "1000",
    "redeliveryPolicy.maximumRedeliveries": "5",
    "redeliveryPolicy.initialRedeliveryDelay": "1000",
    "connection.useAsyncSend": "true",
    "connection.alwaysSessionAsync": "true"
  }
}
```

**Configuration**:
```properties
jms.provider.config.file=activemq.provider.json
jms.initial.context.factory=org.apache.activemq.jndi.ActiveMQInitialContextFactory
jms.provider.url=tcp://localhost:61616
```

### 4. Generic JMS Provider

**File**: `generic.provider.json`

```json
{
  "properties": {
  }
}
```

**Configuration**:
```properties
jms.provider.config.file=generic.provider.json
```

## How It Works

The provider configuration system works through these simple steps:

### 1. Configuration
Set the provider file in `application.properties`:
```properties
jms.provider.config.file=weblogic.provider.json
```

### 2. Loading
`JsonProviderConfig` loads the specified JSON file from the `config/` directory.

### 3. Property Application
All properties from the JSON file's "properties" object are applied directly to the JNDI environment hashtable.

### 4. JNDI Context Creation
The JNDI context is created with the enhanced environment containing all provider-specific properties.

## Example Usage

### Switching Between Providers

**For WebLogic**:
```properties
# application.properties
jms.provider.config.file=weblogic.provider.json
jms.initial.context.factory=weblogic.jndi.WLInitialContextFactory
jms.provider.url=t3://localhost:7001
```

**For Artemis**:
```properties
# application-artemis.properties  
jms.provider.config.file=artemis.provider.json
jms.initial.context.factory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory
jms.provider.url=tcp://localhost:61616
```

### Environment-Specific Overrides

Create environment-specific provider files:

**weblogic-dev.provider.json**:
```json
{
  "properties": {
    "weblogic.jndi.connectTimeout": "5000",
    "weblogic.jndi.readTimeout": "10000"
  }
}
```

**weblogic-prod.provider.json**:
```json
{
  "properties": {
    "weblogic.jndi.connectTimeout": "30000", 
    "weblogic.jndi.readTimeout": "60000",
    "weblogic.jndi.connectionRetryCount": "5"
  }
}
```

## Adding New Providers

### 1. Create Provider JSON File

Create a new JSON file in `src/main/resources/config/` with your provider-specific properties:

**Example**: `myprovider.provider.json`
```json
{
  "properties": {
    "com.myprovider.timeout": "30000",
    "com.myprovider.retries": "3", 
    "com.myprovider.poolsize": "10",
    "com.myprovider.compression": "true",
    "custom.property": "customValue"
  }
}
```

### 2. Configure Application Properties

Update your `application.properties` to use the new provider:

```properties
# My Provider Configuration
jms.provider.config.file=myprovider.provider.json
jms.initial.context.factory=com.myprovider.jndi.InitialContextFactory
jms.provider.url=custom://localhost:9999
```

### 3. Test the Configuration

Create a test to verify your provider configuration:

```java
@Test
public void testMyProviderConfiguration() {
    JMSConfig config = new JMSConfig("myprovider-test.properties");
    JsonProviderConfig provider = new JsonProviderConfig(config);
    
    assertEquals("Myprovider", provider.getProviderName());
    assertTrue(provider.isOptimizationsEnabled());
    
    Hashtable<String, String> env = new Hashtable<>();
    provider.applyJNDIOptimizations(env);
    
    assertEquals("30000", env.get("com.myprovider.timeout"));
    assertEquals("3", env.get("com.myprovider.retries"));
    assertEquals("10", env.get("com.myprovider.poolsize"));
}
```

### 4. No Code Changes Required

That's it! No Java code changes are needed. The `JsonProviderConfig` class will:
- Load your JSON file
- Apply all properties to the JNDI environment
- Provide a provider name based on the filename

## Benefits

### 1. **Simplicity**
- Direct property mapping from JSON to JNDI environment
- No complex detection logic or condition evaluation
- Easy to understand file format

### 2. **No Code Changes**
- Add providers without Java code modifications
- Modify settings without recompilation
- Deploy configuration changes independently

### 3. **Maintainability**
- All provider properties in individual files
- Easy to understand and modify
- Version control friendly

### 4. **Flexibility**
- Create environment-specific provider files
- Override properties per deployment
- Easy switching between providers

### 5. **Extensibility**
- Add new providers by creating JSON files
- Support any JNDI properties
- No limitations on property names or values

## Testing

### Configuration Validation

Test provider configuration loading and property application:

```java
@Test
public void testWebLogicProviderConfiguration() {
    // Setup config with WebLogic provider file
    JMSConfig config = new JMSConfig("weblogic-test.properties");
    // weblogic-test.properties contains: jms.provider.config.file=weblogic.provider.json
    
    JsonProviderConfig provider = new JsonProviderConfig(config);
    
    assertEquals("Weblogic", provider.getProviderName());
    assertTrue(provider.isOptimizationsEnabled());
    
    Hashtable<String, String> env = new Hashtable<>();
    provider.applyJNDIOptimizations(env);
    
    // Verify WebLogic-specific properties are applied
    assertNotNull(env.get("weblogic.jndi.connectTimeout"));
    assertEquals("15000", env.get("weblogic.jndi.connectTimeout"));
    assertEquals("true", env.get("weblogic.jndi.tcpNoDelay"));
}
```

### JSON File Validation

Validate JSON structure and content:

```java
@Test  
public void testProviderJsonFiles() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    
    // Test WebLogic provider file
    JsonNode weblogicConfig = mapper.readTree(
        getClass().getResourceAsStream("/config/weblogic.provider.json"));
    
    assertNotNull(weblogicConfig.get("properties"));
    assertTrue(weblogicConfig.get("properties").has("weblogic.jndi.connectTimeout"));
    
    // Test Artemis provider file
    JsonNode artemisConfig = mapper.readTree(
        getClass().getResourceAsStream("/config/artemis.provider.json"));
    
    assertNotNull(artemisConfig.get("properties"));
    assertTrue(artemisConfig.get("properties").has("connection.ttl"));
}
```

### Provider Switching Test

Test switching between different providers:

```java
@Test
public void testProviderSwitching() {
    // Test WebLogic provider
    JMSConfig weblogicConfig = createConfig("weblogic.provider.json");
    JsonProviderConfig weblogicProvider = new JsonProviderConfig(weblogicConfig);
    assertEquals("Weblogic", weblogicProvider.getProviderName());
    
    // Test Artemis provider
    JMSConfig artemisConfig = createConfig("artemis.provider.json");
    JsonProviderConfig artemisProvider = new JsonProviderConfig(artemisConfig);
    assertEquals("Artemis", artemisProvider.getProviderName());
}

private JMSConfig createConfig(String providerFile) {
    Properties props = new Properties();
    props.setProperty("jms.provider.config.file", providerFile);
    return new JMSConfig(props);
}
```

## Troubleshooting

### Provider File Not Found

**Problem**: `Provider configuration file not found: config/myprovider.provider.json`

**Solutions**:
1. **Check File Location**: Ensure the JSON file is in `src/main/resources/config/`
2. **Verify File Name**: Ensure the filename matches the property exactly
3. **Check Configuration**: Verify `jms.provider.config.file=myprovider.provider.json` in application.properties

### JSON Parsing Errors

**Problem**: `Error loading provider configuration: Unexpected character...`

**Solutions**:
1. **Validate JSON**: Use a JSON validator to check syntax
2. **Check Quotes**: Ensure all strings are properly quoted
3. **Verify Structure**: Ensure the file has the correct "properties" object structure

### No Properties Applied

**Problem**: Provider loads but no JNDI properties are applied

**Solutions**:
1. **Check JSON Structure**: Ensure properties are inside a "properties" object
2. **Verify Property Names**: Check that property names are spelled correctly
3. **Enable Debug Logging**: Add `logging.level.com.example.jms.provider.JsonProviderConfig=DEBUG`

### Example Debug Output

Enable debug logging to see what's happening:

```properties
# In application.properties
logging.level.com.example.jms.provider.JsonProviderConfig=DEBUG
```

Expected debug output:
```
DEBUG c.e.j.p.JsonProviderConfig - Loading provider configuration from: config/weblogic.provider.json
INFO  c.e.j.p.JsonProviderConfig - Successfully loaded provider configuration from: config/weblogic.provider.json
DEBUG c.e.j.p.JsonProviderConfig - Applying JNDI properties from provider file: weblogic.provider.json
DEBUG c.e.j.p.JsonProviderConfig - Applied JNDI property: weblogic.jndi.connectTimeout=15000
DEBUG c.e.j.p.JsonProviderConfig - Applied JNDI property: weblogic.jndi.readTimeout=30000
INFO  c.e.j.p.JsonProviderConfig - Applied 8 JNDI properties from provider file: weblogic.provider.json
```

### Common JSON Structure Issues

**❌ Incorrect**:
```json
{
  "weblogic.jndi.connectTimeout": "15000"
}
```

**✅ Correct**:
```json
{
  "properties": {
    "weblogic.jndi.connectTimeout": "15000"
  }
}
```

This simplified JSON-based approach provides an easy, maintainable way to configure JMS providers without requiring code changes for new providers or property modifications.
