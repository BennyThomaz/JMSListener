package com.example.jms.provider;

import java.util.Hashtable;

/**
 * Interface for JMS provider-specific JNDI configurations.
 * Implementations provide provider-specific optimizations and settings.
 */
public interface JMSProviderConfig {
    
    /**
     * Apply provider-specific JNDI environment configurations
     * 
     * @param env The JNDI environment hashtable to configure
     */
    void applyJNDIOptimizations(Hashtable<String, String> env);
    
    /**
     * Get the provider name for logging and identification
     * 
     * @return The provider name (e.g., "WebLogic", "Artemis")
     */
    String getProviderName();
    
    /**
     * Check if provider-specific optimizations are enabled
     * 
     * @return true if optimizations should be applied
     */
    boolean isOptimizationsEnabled();
}
