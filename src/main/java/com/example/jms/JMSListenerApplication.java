package com.example.jms;

import com.example.jms.callback.ConsoleMessageCallback;
import com.example.jms.callback.MessageCallback;
import com.example.jms.callback.HttpMessageCallback;
import com.example.jms.callback.CompositeMessageCallback;
import com.example.jms.config.JMSConfig;
import com.example.jms.listener.JMSListener;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.util.concurrent.CountDownLatch;

/**
 * Main application class for the JMS Listener.
 * This class handles application startup, shutdown, and signal handling.
 */
public class JMSListenerApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(JMSListenerApplication.class);
    private static final CountDownLatch applicationShutdownLatch = new CountDownLatch(1);
    
    private static JMSListener jmsListener;
    private static MessageCallback messageCallback;
    
    public static void main(String[] args) {
        logger.info("Starting JMS Listener Application...");
        
        try {
            // Load configuration
            String configFile = getConfigFile(args);
            JMSConfig config = new JMSConfig(configFile);
            logger.info("Configuration loaded from: {}", configFile);
            
            // Create message callback
            messageCallback = createMessageCallback(config);
            
            // Create and start JMS listener
            jmsListener = new JMSListener(config, messageCallback);
            
            // Setup shutdown hook
            setupShutdownHook(config);
            
            // Start the listener
            jmsListener.start();
            
            logger.info("JMS Listener Application started successfully");
            logger.info("Application is running. Press Ctrl+C to shutdown gracefully.");
            
            // Start heartbeat thread
            startHeartbeatThread(config);
            
            // Wait for shutdown signal
            waitForShutdownSignal();
            
        } catch (ConfigurationException e) {
            logger.error("Configuration error: {}", e.getMessage());
            System.exit(1);
        } catch (NamingException e) {
            logger.error("JNDI error: {}", e.getMessage());
            System.exit(2);
        } catch (JMSException e) {
            logger.error("JMS error: {}", e.getMessage());
            System.exit(3);
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            System.exit(4);
        }
    }
    
    /**
     * Get configuration file from command line arguments or use default
     */
    private static String getConfigFile(String[] args) {
        if (args.length > 0) {
            return args[0];
        }
        return "application.properties";
    }
    
    /**
     * Create message callback based on configuration
     */
    private static MessageCallback createMessageCallback(JMSConfig config) {
        CompositeMessageCallback composite = new CompositeMessageCallback();
        
        // Add console callback if enabled
        if (config.isConsoleCallbackEnabled()) {
            composite.addCallback(new ConsoleMessageCallback());
            logger.info("Added console message callback");
        } else {
            logger.info("Console callback disabled in configuration");
        }
        
        // Add HTTP callback if enabled
        if (config.isHttpCallbackEnabled()) {
            String httpUrl = config.getHttpCallbackUrl();
            if (httpUrl != null && !httpUrl.trim().isEmpty()) {
                try {
                    HttpMessageCallback httpCallback = new HttpMessageCallback(config);
                    composite.addCallback(httpCallback);
                    logger.info("Added HTTP message callback for URL: {}", httpUrl);
                } catch (Exception e) {
                    logger.error("Failed to create HTTP callback: {}", e.getMessage());
                    // Continue with other callbacks
                }
            } else {
                logger.warn("HTTP callback enabled but no URL configured");
            }
        } else {
            logger.info("HTTP callback disabled in configuration");
        }
        
        // Ensure at least one callback is configured
        if (composite.getCallbackCount() == 0) {
            logger.warn("No callbacks configured! Adding console callback as fallback");
            composite.addCallback(new ConsoleMessageCallback());
        }
        
        return composite;
    }
    
    /**
     * Setup shutdown hook for graceful application termination
     */
    private static void setupShutdownHook(JMSConfig config) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received, initiating graceful shutdown...");
            
            if (jmsListener != null && jmsListener.isRunning()) {
                jmsListener.stop();
                
                // Wait for listener to shutdown with timeout
                try {
                    boolean shutdownComplete = jmsListener.awaitShutdown(config.getShutdownTimeout());
                    if (shutdownComplete) {
                        logger.info("JMS Listener shutdown completed");
                    } else {
                        logger.warn("JMS Listener shutdown timed out after {} ms", config.getShutdownTimeout());
                    }
                } catch (InterruptedException e) {
                    logger.warn("Shutdown interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
            
            // Clean up message callback resources
            if (messageCallback instanceof CompositeMessageCallback) {
                try {
                    ((CompositeMessageCallback) messageCallback).close();
                    logger.info("Message callback resources cleaned up");
                } catch (Exception e) {
                    logger.warn("Error cleaning up message callback", e);
                }
            }
            
            logger.info("Application shutdown complete");
            applicationShutdownLatch.countDown();
        }, "ShutdownHook"));
    }
    
    /**
     * Start heartbeat thread to periodically log application status
     */
    private static void startHeartbeatThread(JMSConfig config) {
        Thread heartbeatThread = new Thread(() -> {
            long interval = config.getHeartbeatInterval();
            logger.debug("Starting heartbeat thread with interval: {} ms", interval);
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(interval);
                    
                    if (jmsListener != null && jmsListener.isRunning()) {
                        logger.debug("Application heartbeat - JMS Listener is running");
                    } else {
                        logger.warn("Application heartbeat - JMS Listener is not running");
                        break;
                    }
                    
                } catch (InterruptedException e) {
                    logger.debug("Heartbeat thread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            logger.debug("Heartbeat thread stopped");
        }, "HeartbeatThread");
        
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }
    
    /**
     * Wait for shutdown signal
     */
    private static void waitForShutdownSignal() {
        try {
            applicationShutdownLatch.await();
        } catch (InterruptedException e) {
            logger.warn("Main thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get the JMS listener instance (for testing purposes)
     */
    public static JMSListener getJmsListener() {
        return jmsListener;
    }
}
