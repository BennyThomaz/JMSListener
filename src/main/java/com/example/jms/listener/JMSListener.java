package com.example.jms.listener;

import com.example.jms.callback.MessageCallback;
import com.example.jms.config.JMSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Production-ready JMS Listener for WebLogic Server.
 * This class provides robust message listening capabilities with proper error handling,
 * connection management, and graceful shutdown.
 */
public class JMSListener implements MessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(JMSListener.class);
    
    private final JMSConfig config;
    private final MessageCallback messageCallback;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    
    private Context jndiContext;
    private ConnectionFactory connectionFactory;
    private Queue queue;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    
    public JMSListener(JMSConfig config, MessageCallback messageCallback) {
        this.config = config;
        this.messageCallback = messageCallback;
    }
    
    /**
     * Starts the JMS listener
     */
    public void start() throws JMSException, NamingException {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting JMS Listener...");
            
            try {
                initializeJNDI();
                initializeJMS();
                startListening();
                
                logger.info("JMS Listener started successfully");
                logger.info("Listening on queue: {}", config.getQueueJndi());
                logger.info("Provider URL: {}", config.getProviderUrl());
                
            } catch (Exception e) {
                logger.error("Failed to start JMS Listener", e);
                cleanup();
                running.set(false);
                throw e;
            }
        } else {
            logger.warn("JMS Listener is already running");
        }
    }
    
    /**
     * Stops the JMS listener gracefully
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping JMS Listener...");
            
            try {
                cleanup();
                logger.info("JMS Listener stopped successfully");
            } catch (Exception e) {
                logger.error("Error during JMS Listener shutdown", e);
            } finally {
                shutdownLatch.countDown();
            }
        } else {
            logger.warn("JMS Listener is not running");
        }
    }
    
    /**
     * Waits for the listener to shutdown
     */
    public void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }
    
    /**
     * Waits for the listener to shutdown with timeout
     */
    public boolean awaitShutdown(long timeoutMs) throws InterruptedException {
        return shutdownLatch.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * Checks if the listener is running
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Initialize JNDI context
     */
    private void initializeJNDI() throws NamingException {
        logger.debug("Initializing JNDI context...");
        
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, config.getInitialContextFactory());
        env.put(Context.PROVIDER_URL, config.getProviderUrl());
        
        // Timeout configuration - comprehensive WebLogic settings
        env.put("weblogic.jndi.connectTimeout", String.valueOf(config.getJndiTimeout()));
        env.put("weblogic.jndi.readTimeout", String.valueOf(config.getJndiReadTimeout()));
        env.put("weblogic.jndi.connectionRetryCount", "3");
        env.put("weblogic.jndi.connectionRetryInterval", "5000");

        // Additional WebLogic timeout and retry settings
        //env.put("weblogic.jndi.tcpNoDelay", "true");
        //env.put("weblogic.jndi.WLContext.ENABLE_SERVER_AFFINITY", "false");
        
        // Socket-level timeout settings
        env.put("weblogic.socket.ConnectTimeout", String.valueOf(config.getJndiTimeout()));
        env.put("weblogic.socket.SocketTimeout", String.valueOf(config.getJndiReadTimeout()));
        
        if (config.getSecurityPrincipal() != null) {
            env.put(Context.SECURITY_PRINCIPAL, config.getSecurityPrincipal());
        }
        if (config.getSecurityCredentials() != null) {
            env.put(Context.SECURITY_CREDENTIALS, config.getSecurityCredentials());
        }
        
        logger.debug("JNDI timeout configuration: connectTimeout={}ms, readTimeout={}ms, socketTimeout={}ms", 
                    config.getJndiTimeout(), config.getJndiReadTimeout(), config.getJndiReadTimeout());
        logger.info("Attempting JNDI connection to: {}", config.getProviderUrl());
        
        jndiContext = new InitialContext(env);
        logger.debug("JNDI context initialized");
    }
    
    /**
     * Initialize JMS components
     */
    private void initializeJMS() throws NamingException, JMSException {
        logger.debug("Initializing JMS components...");
        
        // Lookup connection factory and queue
        connectionFactory = (ConnectionFactory) jndiContext.lookup(config.getConnectionFactoryJndi());
        queue = (Queue) jndiContext.lookup(config.getQueueJndi());
        
        // Create connection
        connection = connectionFactory.createConnection();
        connection.setExceptionListener(new JMSExceptionListener());
        
        // Create session
        session = connection.createSession(
            config.isSessionTransacted(),
            config.getMessageAcknowledgmentMode()
        );
        
        // Create consumer
        String messageSelector = config.getMessageSelector();
        if (messageSelector != null && !messageSelector.trim().isEmpty()) {
            consumer = session.createConsumer(queue, messageSelector, config.isNoLocal());
            logger.debug("Created consumer with message selector: {}", messageSelector);
        } else {
            consumer = session.createConsumer(queue);
            logger.debug("Created consumer without message selector");
        }
        
        logger.debug("JMS components initialized");
    }
    
    /**
     * Start listening for messages
     */
    private void startListening() throws JMSException {
        logger.debug("Starting message consumption...");
        
        consumer.setMessageListener(this);
        connection.start();
        
        logger.debug("Message consumption started");
    }
    
    /**
     * MessageListener implementation with enhanced transaction support
     */
    @Override
    public void onMessage(Message message) {
        if (!running.get()) {
            logger.warn("Received message while shutting down, ignoring");
            return;
        }
        
        String messageId = null;
        long processingStartTime = System.currentTimeMillis();
        boolean transactionActive = false;
        
        try {
            messageId = message.getJMSMessageID();
            transactionActive = config.isSessionTransacted();
            
            logger.debug("Processing message: {} (transacted: {})", messageId, transactionActive);
            
            if (transactionActive) {
                logger.debug("Transaction started for message: {}", messageId);
            }
            
            // Invoke the callback - this may throw JMSException if HTTP callback fails
            messageCallback.onMessage(message);
            
            // Manual acknowledgment if required
            if (config.getMessageAcknowledgmentMode() == Session.CLIENT_ACKNOWLEDGE) {
                message.acknowledge();
                logger.debug("Message acknowledged: {}", messageId);
            }
            
            // Commit transaction if session is transacted
            if (transactionActive) {
                session.commit();
                long processingTime = System.currentTimeMillis() - processingStartTime;
                logger.info("Transaction committed successfully for message: {} (processing time: {}ms)", 
                           messageId, processingTime);
            }
            
            logger.debug("Message processed successfully: {}", messageId);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - processingStartTime;
            logger.error("Error processing message: {} after {}ms - {}", messageId, processingTime, e.getMessage(), e);
            
            // Handle transaction rollback if session is transacted
            if (transactionActive) {
                try {
                    // Check if this was an HTTP callback failure and apply delay if configured
                    boolean isHttpFailure = e instanceof JMSException && 
                                          e.getMessage() != null && 
                                          e.getMessage().contains("HTTP callback failed");
                    
                    if (isHttpFailure && config.isTransactionRollbackDelayEnabled()) {
                        long delayMs = config.getTransactionRollbackDelay();
                        logger.warn("HTTP callback failed for message: {} - applying rollback delay of {}ms to prevent rapid retry", 
                                   messageId, delayMs);
                        
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            logger.warn("Rollback delay interrupted for message: {}", messageId);
                        }
                    }
                    
                    session.rollback();
                    logger.warn("Transaction rolled back due to message processing error for message: {} (processing time: {}ms)", 
                               messageId, processingTime);
                    
                    // Log specific details if this was an HTTP callback failure
                    if (isHttpFailure) {
                        logger.error("HTTP callback failure triggered transaction rollback for message: {} - message will be redelivered", messageId);
                    }
                    
                    // Check if this might be a repeated failure (poison message scenario)
                    try {
                        if (message.propertyExists("JMSXDeliveryCount")) {
                            int redeliveryCount = message.getIntProperty("JMSXDeliveryCount");
                            if (redeliveryCount > 3) {
                                logger.warn("Message {} has been redelivered {} times - potential poison message", 
                                           messageId, redeliveryCount);
                            }
                        }
                    } catch (JMSException propException) {
                        logger.debug("Could not check redelivery count for message: {}", messageId);
                    }
                    
                } catch (JMSException rollbackException) {
                    logger.error("Critical error: Failed to rollback transaction for message: {} - {}", 
                                messageId, rollbackException.getMessage(), rollbackException);
                    // This is a serious issue - the transaction state is now uncertain
                }
            } else {
                // If not transacted, log that message processing failed but cannot be rolled back
                logger.warn("Message processing failed for message: {} but session is not transacted, cannot rollback", messageId);
            }
        }
    }
    
    /**
     * Cleanup resources
     */
    private void cleanup() {
        // Close consumer
        if (consumer != null) {
            try {
                consumer.close();
                logger.debug("Consumer closed");
            } catch (JMSException e) {
                logger.warn("Error closing consumer", e);
            }
        }
        
        // Close session
        if (session != null) {
            try {
                session.close();
                logger.debug("Session closed");
            } catch (JMSException e) {
                logger.warn("Error closing session", e);
            }
        }
        
        // Close connection
        if (connection != null) {
            try {
                connection.close();
                logger.debug("Connection closed");
            } catch (JMSException e) {
                logger.warn("Error closing connection", e);
            }
        }
        
        // Close JNDI context
        if (jndiContext != null) {
            try {
                jndiContext.close();
                logger.debug("JNDI context closed");
            } catch (NamingException e) {
                logger.warn("Error closing JNDI context", e);
            }
        }
    }
    
    /**
     * JMS Exception Listener for connection monitoring
     */
    private class JMSExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException exception) {
            logger.error("JMS Connection Exception occurred", exception);
            
            // In production, you might want to implement reconnection logic here
            if (running.get()) {
                logger.error("JMS connection lost, stopping listener");
                stop();
            }
        }
    }
}
