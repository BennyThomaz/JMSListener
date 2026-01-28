package com.example.jms.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Configuration manager for the JMS listener application.
 * Handles loading and accessing configuration properties.
 */
public class JMSConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(JMSConfig.class);
    private static final String DEFAULT_CONFIG_FILE = "application.properties";
    
    private final Configuration config;
    
    public JMSConfig() throws ConfigurationException {
        this(DEFAULT_CONFIG_FILE);
    }
    
    public JMSConfig(String configFile) throws ConfigurationException {
        Configurations configs = new Configurations();
        Configuration tempConfig = null;
        
        try {
            // Try to load from classpath first
            tempConfig = configs.properties(configFile);
            logger.info("Loaded configuration from classpath: {}", configFile);
        } catch (ConfigurationException e) {
            // Try to load from file system
            File file = new File(configFile);
            if (file.exists()) {
                tempConfig = configs.properties(file);
                logger.info("Loaded configuration from file: {}", file.getAbsolutePath());
            } else {
                logger.error("Configuration file not found: {}", configFile);
                throw e;
            }
        }
        
        this.config = tempConfig;
    }
    
    // JMS Connection Properties
    public String getConnectionFactoryJndi() {
        return config.getString("jms.connection.factory.jndi", "weblogic.jms.ConnectionFactory");
    }
    
    public String getQueueJndi() {
        return config.getString("jms.queue.jndi", "jms/MyQueue");
    }
    
    public String getInitialContextFactory() {
        return config.getString("jms.initial.context.factory", "weblogic.jndi.WLInitialContextFactory");
    }
    
    public String getProviderUrl() {
        return config.getString("jms.provider.url", "t3://localhost:7001");
    }
    
    public String getSecurityPrincipal() {
        return config.getString("jms.security.principal");
    }
    
    public String getSecurityCredentials() {
        return config.getString("jms.security.credentials");
    }
    
    // Provider Configuration
    public String getProviderConfigFile() {
        return config.getString("jms.provider.config.file", "generic.provider.json");
    }
    
    // JNDI Timeout Properties
    public long getJndiTimeout() {
        return config.getLong("jms.jndi.timeout", 30000L); // Default 30 seconds
    }
    
    public long getJndiReadTimeout() {
        return config.getLong("jms.jndi.read.timeout", 60000L); // Default 60 seconds
    }
    
    // Connection Pool Properties
    public int getConnectionPoolSize() {
        return config.getInt("jms.connection.pool.size", 5);
    }
    
    public long getConnectionPoolMaxIdle() {
        return config.getLong("jms.connection.pool.max.idle", 60000);
    }
    
    public long getConnectionPoolValidationInterval() {
        return config.getLong("jms.connection.pool.validation.interval", 30000);
    }
    
    // Message Processing Properties
    public int getMessageAcknowledgmentMode() {
        String mode = config.getString("jms.message.acknowledgment.mode", "AUTO_ACKNOWLEDGE");
        switch (mode.toUpperCase()) {
            case "AUTO_ACKNOWLEDGE":
                return javax.jms.Session.AUTO_ACKNOWLEDGE;
            case "CLIENT_ACKNOWLEDGE":
                return javax.jms.Session.CLIENT_ACKNOWLEDGE;
            case "DUPS_OK_ACKNOWLEDGE":
                return javax.jms.Session.DUPS_OK_ACKNOWLEDGE;
            default:
                logger.warn("Unknown acknowledgment mode: {}, using AUTO_ACKNOWLEDGE", mode);
                return javax.jms.Session.AUTO_ACKNOWLEDGE;
        }
    }
    
    public boolean isSessionTransacted() {
        return config.getBoolean("jms.session.transacted", false);
    }
    
    public String getMessageSelector() {
        return config.getString("jms.consumer.message.selector");
    }
    
    public boolean isNoLocal() {
        return config.getBoolean("jms.consumer.no.local", false);
    }
    
    // Application Properties
    public long getShutdownTimeout() {
        return config.getLong("app.shutdown.timeout", 30000);
    }
    
    public long getHeartbeatInterval() {
        return config.getLong("app.heartbeat.interval", 60000);
    }
    
    public int getMaxRetryAttempts() {
        return config.getInt("app.max.retry.attempts", 3);
    }
    
    public long getRetryDelay() {
        return config.getLong("app.retry.delay", 5000);
    }
    
    // HTTP Callback Properties
    public boolean isHttpCallbackEnabled() {
        return config.getBoolean("http.callback.enabled", false);
    }
    
    public String getHttpCallbackUrl() {
        return config.getString("http.callback.url");
    }
    
    public String getHttpCallbackMethod() {
        return config.getString("http.callback.method", "POST");
    }
    
    public String getHttpCallbackContentType() {
        return config.getString("http.callback.content.type", "application/json");
    }
    
    public int getHttpCallbackConnectionTimeout() {
        return config.getInt("http.callback.timeout.connection", 5000);
    }
    
    public int getHttpCallbackSocketTimeout() {
        return config.getInt("http.callback.timeout.socket", 10000);
    }
    
    public int getHttpCallbackRetryAttempts() {
        return config.getInt("http.callback.retry.attempts", 3);
    }
    
    public long getHttpCallbackRetryDelay() {
        return config.getLong("http.callback.retry.delay", 1000);
    }
    
    public String getHttpCallbackHeader(String headerName) {
        return config.getString("http.callback.headers." + headerName.toLowerCase());
    }
    
    // Console Callback Properties
    public boolean isConsoleCallbackEnabled() {
        return config.getBoolean("console.callback.enabled", true);
    }
    
    /**
     * Get a custom property value
     */
    public String getProperty(String key) {
        return config.getString(key);
    }
    
    /**
     * Get a custom property value with default
     */
    public String getProperty(String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }
    
    // Transaction Rollback Delay Settings
    public long getTransactionRollbackDelay() {
        return config.getLong("jms.transaction.rollback.delay", 0L);
    }
    
    public boolean isTransactionRollbackDelayEnabled() {
        return getTransactionRollbackDelay() > 0;
    }

    // Idle Reconnect Settings
    public long getIdleReconnectInterval() {
        return config.getLong("jms.idle.reconnect.interval", 1800000L); // Default 30 minutes
    }

    public boolean isIdleReconnectEnabled() {
        return getIdleReconnectInterval() > 0;
    }
}
