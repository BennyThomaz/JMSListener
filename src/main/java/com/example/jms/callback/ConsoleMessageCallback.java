package com.example.jms.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.Date;

/**
 * Default implementation of MessageCallback that prints message details to console.
 * This implementation handles different types of JMS messages and logs their content.
 */
public class ConsoleMessageCallback implements MessageCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsoleMessageCallback.class);
    
    @Override
    public void onMessage(Message message) throws JMSException {
        try {
            String messageInfo = formatMessage(message);
            
            // Print to console
            System.out.println("=== JMS Message Received ===");
            System.out.println("Timestamp: " + new Date());
            System.out.println(messageInfo);
            System.out.println("===========================");
            
            // Log the message
            logger.info("Message received: {}", messageInfo);
            
        } catch (Exception e) {
            logger.error("Error processing message", e);
            throw new JMSException("Failed to process message: " + e.getMessage());
        }
    }
    
    /**
     * Formats the JMS message for display
     */
    private String formatMessage(Message message) throws JMSException {
        StringBuilder sb = new StringBuilder();
        
        // Message ID and Type
        sb.append("Message ID: ").append(message.getJMSMessageID()).append("\n");
        sb.append("Message Type: ").append(message.getClass().getSimpleName()).append("\n");
        
        // Destination
        if (message.getJMSDestination() != null) {
            sb.append("Destination: ").append(message.getJMSDestination()).append("\n");
        }
        
        // Timestamp
        if (message.getJMSTimestamp() > 0) {
            sb.append("JMS Timestamp: ").append(new Date(message.getJMSTimestamp())).append("\n");
        }
        
        // Correlation ID
        if (message.getJMSCorrelationID() != null) {
            sb.append("Correlation ID: ").append(message.getJMSCorrelationID()).append("\n");
        }
        
        // Reply To
        if (message.getJMSReplyTo() != null) {
            sb.append("Reply To: ").append(message.getJMSReplyTo()).append("\n");
        }
        
        // Message content based on type
        sb.append("Content: ");
        if (message instanceof TextMessage) {
            sb.append(((TextMessage) message).getText());
        } else if (message instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) message;
            sb.append("Bytes message with length: ").append(bytesMessage.getBodyLength());
        } else if (message instanceof MapMessage) {
            MapMessage mapMessage = (MapMessage) message;
            sb.append("Map message with keys: ");
            // Note: In production, you might want to iterate through all map names
            sb.append("[Map content - implement enumeration if needed]");
        } else if (message instanceof ObjectMessage) {
            sb.append("Object message: ").append(((ObjectMessage) message).getObject());
        } else if (message instanceof StreamMessage) {
            sb.append("Stream message");
        } else {
            sb.append("Unknown message type");
        }
        
        return sb.toString();
    }
}
