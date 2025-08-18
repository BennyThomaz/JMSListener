package com.example.jms.callback;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * Callback interface for handling JMS messages.
 * Implementations of this interface define how messages are processed
 * when received by the JMS listener.
 */
@FunctionalInterface
public interface MessageCallback {
    
    /**
     * Processes a received JMS message.
     * 
     * @param message the JMS message to process
     * @throws JMSException if an error occurs while processing the message
     */
    void onMessage(Message message) throws JMSException;
}
