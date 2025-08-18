package com.example.jms.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Composite message callback that can execute multiple callbacks in sequence.
 * This allows for combining different callback implementations (e.g., console + HTTP).
 */
public class CompositeMessageCallback implements MessageCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(CompositeMessageCallback.class);
    
    private final List<MessageCallback> callbacks;
    private final boolean continueOnError;
    
    public CompositeMessageCallback(boolean continueOnError, MessageCallback... callbacks) {
        this.callbacks = new ArrayList<>(Arrays.asList(callbacks));
        this.continueOnError = continueOnError;
        logger.info("CompositeMessageCallback initialized with {} callbacks, continueOnError: {}", 
                   this.callbacks.size(), continueOnError);
    }
    
    public CompositeMessageCallback(MessageCallback... callbacks) {
        this(true, callbacks); // Default to continue on error
    }
    
    /**
     * Add a callback to the composition
     */
    public void addCallback(MessageCallback callback) {
        callbacks.add(callback);
        logger.debug("Added callback: {}", callback.getClass().getSimpleName());
    }
    
    /**
     * Remove a callback from the composition
     */
    public void removeCallback(MessageCallback callback) {
        callbacks.remove(callback);
        logger.debug("Removed callback: {}", callback.getClass().getSimpleName());
    }
    
    @Override
    public void onMessage(Message message) throws JMSException {
        if (callbacks.isEmpty()) {
            logger.warn("No callbacks configured, message will be ignored");
            return;
        }
        
        List<Exception> errors = new ArrayList<>();
        
        for (int i = 0; i < callbacks.size(); i++) {
            MessageCallback callback = callbacks.get(i);
            try {
                logger.debug("Executing callback {} of {}: {}", 
                           i + 1, callbacks.size(), callback.getClass().getSimpleName());
                
                callback.onMessage(message);
                
                logger.debug("Callback {} completed successfully", callback.getClass().getSimpleName());
                
            } catch (Exception e) {
                String errorMsg = String.format("Callback %s failed: %s", 
                                               callback.getClass().getSimpleName(), e.getMessage());
                logger.error(errorMsg, e);
                errors.add(e);
                
                if (!continueOnError) {
                    logger.error("Stopping callback execution due to error in {}", 
                               callback.getClass().getSimpleName());
                    break;
                }
            }
        }
        
        // If there were errors and we're not continuing on error, or if all callbacks failed
        if (!errors.isEmpty()) {
            if (!continueOnError || errors.size() == callbacks.size()) {
                StringBuilder errorMsg = new StringBuilder("Callback execution failed. Errors: ");
                for (int i = 0; i < errors.size(); i++) {
                    if (i > 0) errorMsg.append("; ");
                    errorMsg.append(errors.get(i).getMessage());
                }
                throw new JMSException(errorMsg.toString());
            } else {
                logger.warn("Some callbacks failed, but continuing due to continueOnError=true. " +
                           "Failed: {}, Successful: {}", errors.size(), callbacks.size() - errors.size());
            }
        }
    }
    
    /**
     * Get the number of configured callbacks
     */
    public int getCallbackCount() {
        return callbacks.size();
    }
    
    /**
     * Check if the composite has any callbacks
     */
    public boolean hasCallbacks() {
        return !callbacks.isEmpty();
    }
    
    /**
     * Close any resources used by the callbacks
     */
    public void close() {
        for (MessageCallback callback : callbacks) {
            try {
                if (callback instanceof HttpMessageCallback) {
                    ((HttpMessageCallback) callback).close();
                }
                // Add other cleanup logic for other callback types as needed
            } catch (Exception e) {
                logger.warn("Error closing callback {}: {}", 
                           callback.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}
