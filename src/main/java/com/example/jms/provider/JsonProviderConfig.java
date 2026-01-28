package com.example.jms.provider;

import com.example.jms.config.JMSConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * JSON-based JMS provider configuration loader.
 * Loads provider configuration from individual provider JSON files specified in application.properties.
 * Each provider file contains a simple "properties" object with key-value pairs for JNDI environment.
 */
public class JsonProviderConfig implements JMSProviderConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonProviderConfig.class);
    
    private final JsonNode providerConfig;
    private final String providerFileName;
    
    public JsonProviderConfig(JMSConfig config) {
        this.providerFileName = config.getProviderConfigFile();
        this.providerConfig = loadProviderConfig();
    }
    
    @Override
    public void applyJNDIOptimizations(Hashtable<String, String> env) {
        if (providerConfig == null) {
            logger.debug("No provider configuration available for file: {}", providerFileName);
            return;
        }
        
        logger.debug("Applying JNDI properties from provider file: {}", providerFileName);
        
        // Get the properties object from the JSON
        JsonNode properties = providerConfig.get("properties");
        if (properties == null) {
            logger.warn("No 'properties' section found in provider file: {}", providerFileName);
            return;
        }
        
        // Apply all properties directly to the JNDI environment
        Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
        int appliedCount = 0;
        
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            String value = entry.getValue().asText();
            
            if (value != null && !value.trim().isEmpty()) {
                env.put(key, value);
                logger.debug("Applied JNDI property: {}={}", key, value);
                appliedCount++;
            } else {
                logger.debug("Skipped empty property: {}", key);
            }
        }
        
        logger.info("Applied {} JNDI properties from provider file: {}", appliedCount, providerFileName);
    }
    
    /**
     * Loads the provider configuration from the JSON file specified in application.properties
     */
    private JsonNode loadProviderConfig() {
        if (providerFileName == null || providerFileName.trim().isEmpty()) {
            logger.warn("No provider configuration file specified in application.properties (jms.provider.config.file)");
            return null;
        }
        
        String configPath = "config/" + providerFileName;
        logger.debug("Loading provider configuration from: {}", configPath);
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream configStream = getClass().getClassLoader().getResourceAsStream(configPath);
            
            if (configStream == null) {
                logger.error("Provider configuration file not found: {}", configPath);
                return null;
            }
            
            JsonNode config = mapper.readTree(configStream);
            logger.info("Successfully loaded provider configuration from: {}", configPath);
            return config;
            
        } catch (IOException e) {
            logger.error("Error loading provider configuration from {}: {}", configPath, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public String getProviderName() {
        if (providerFileName != null) {
            // Extract provider name from filename (e.g., "weblogic.provider.json" -> "WebLogic")
            String name = providerFileName.replace(".provider.json", "");
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return "Unknown";
    }
    
    @Override
    public boolean isOptimizationsEnabled() {
        // Optimizations are enabled if we have a valid provider configuration
        return providerConfig != null;
    }
}
