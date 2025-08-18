package com.example.jms.callback;

import com.example.jms.config.JMSConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * HTTP callback implementation that posts JMS messages to a configured HTTP endpoint.
 * This implementation converts JMS messages to JSON and sends them via HTTP POST/PUT.
 */
public class HttpMessageCallback implements MessageCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpMessageCallback.class);
    
    private final JMSConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public HttpMessageCallback(JMSConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        
        // Configure HTTP client with timeouts
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getHttpCallbackConnectionTimeout()))
                .build();
        
        logger.info("HttpMessageCallback initialized with URL: {}", config.getHttpCallbackUrl());
    }
    
    @Override
    public void onMessage(Message message) throws JMSException {
        if (!config.isHttpCallbackEnabled()) {
            logger.debug("HTTP callback is disabled, skipping message processing");
            return;
        }
        
        String url = config.getHttpCallbackUrl();
        if (url == null || url.trim().isEmpty()) {
            logger.warn("HTTP callback URL not configured, skipping message processing");
            return;
        }
        
        try {
            // Convert JMS message to JSON
            String jsonPayload = convertMessageToJson(message);
            
            // Send HTTP request with retry logic
            boolean success = sendHttpRequestWithRetry(url, jsonPayload);
            
            if (success) {
                logger.info("Message successfully sent to HTTP endpoint: {}", message.getJMSMessageID());
            } else {
                logger.error("Failed to send message to HTTP endpoint after retries: {}", message.getJMSMessageID());
                throw new JMSException("HTTP callback failed for message: " + message.getJMSMessageID());
            }
            
        } catch (Exception e) {
            logger.error("Error processing message for HTTP callback", e);
            throw new JMSException("Failed to process message for HTTP callback: " + e.getMessage());
        }
    }
    
    /**
     * Convert JMS message to JSON format
     */
    private String convertMessageToJson(Message message) throws JMSException {
        try {
            ObjectNode jsonMessage = objectMapper.createObjectNode();
            
            // Message metadata
            jsonMessage.put("messageId", message.getJMSMessageID());
            jsonMessage.put("timestamp", System.currentTimeMillis());
            jsonMessage.put("messageType", message.getClass().getSimpleName());
            
            if (message.getJMSTimestamp() > 0) {
                jsonMessage.put("jmsTimestamp", message.getJMSTimestamp());
            }
            
            if (message.getJMSCorrelationID() != null) {
                jsonMessage.put("correlationId", message.getJMSCorrelationID());
            }
            
            if (message.getJMSDestination() != null) {
                jsonMessage.put("destination", message.getJMSDestination().toString());
            }
            
            if (message.getJMSReplyTo() != null) {
                jsonMessage.put("replyTo", message.getJMSReplyTo().toString());
            }
            
            jsonMessage.put("deliveryMode", message.getJMSDeliveryMode());
            jsonMessage.put("priority", message.getJMSPriority());
            jsonMessage.put("expiration", message.getJMSExpiration());
            jsonMessage.put("redelivered", message.getJMSRedelivered());
            
            // Message properties
            ObjectNode properties = objectMapper.createObjectNode();
            Enumeration<?> propertyNames = message.getPropertyNames();
            while (propertyNames.hasMoreElements()) {
                String propertyName = (String) propertyNames.nextElement();
                Object propertyValue = message.getObjectProperty(propertyName);
                if (propertyValue != null) {
                    properties.put(propertyName, propertyValue.toString());
                }
            }
            jsonMessage.set("properties", properties);
            
            // Message content based on type
            if (message instanceof TextMessage) {
                jsonMessage.put("contentType", "text");
                jsonMessage.put("content", ((TextMessage) message).getText());
            } else if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) message;
                jsonMessage.put("contentType", "bytes");
                jsonMessage.put("contentLength", bytesMessage.getBodyLength());
                
                // Read bytes content (be careful with large messages)
                long bodyLength = bytesMessage.getBodyLength();
                if (bodyLength > 0 && bodyLength <= 1024 * 1024) { // Limit to 1MB
                    byte[] bytes = new byte[(int) bodyLength];
                    bytesMessage.readBytes(bytes);
                    jsonMessage.put("content", Base64.getEncoder().encodeToString(bytes));
                } else {
                    jsonMessage.put("content", "Binary content too large or empty");
                }
            } else if (message instanceof MapMessage) {
                MapMessage mapMessage = (MapMessage) message;
                jsonMessage.put("contentType", "map");
                
                ObjectNode mapContent = objectMapper.createObjectNode();
                Enumeration<?> mapNames = mapMessage.getMapNames();
                while (mapNames.hasMoreElements()) {
                    String mapName = (String) mapNames.nextElement();
                    Object mapValue = mapMessage.getObject(mapName);
                    if (mapValue != null) {
                        mapContent.put(mapName, mapValue.toString());
                    }
                }
                jsonMessage.set("content", mapContent);
            } else if (message instanceof ObjectMessage) {
                jsonMessage.put("contentType", "object");
                Object obj = ((ObjectMessage) message).getObject();
                jsonMessage.put("content", obj != null ? obj.toString() : "null");
            } else if (message instanceof StreamMessage) {
                jsonMessage.put("contentType", "stream");
                jsonMessage.put("content", "Stream message content");
            } else {
                jsonMessage.put("contentType", "unknown");
                jsonMessage.put("content", "Unknown message type");
            }
            
            return objectMapper.writeValueAsString(jsonMessage);
            
        } catch (Exception e) {
            logger.error("Error converting message to JSON", e);
            throw new JMSException("Failed to convert message to JSON: " + e.getMessage());
        }
    }
    
    /**
     * Send HTTP request with retry logic and detailed failure tracking
     */
    private boolean sendHttpRequestWithRetry(String url, String jsonPayload) {
        int maxAttempts = config.getHttpCallbackRetryAttempts();
        long retryDelay = config.getHttpCallbackRetryDelay();
        StringBuilder failureDetails = new StringBuilder();
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                boolean success = sendHttpRequest(url, jsonPayload);
                if (success) {
                    if (attempt > 1) {
                        logger.info("HTTP request succeeded on attempt {} after previous failures: {}", 
                                   attempt, failureDetails.toString());
                    }
                    return true;
                }
                
                // Record this failure
                failureDetails.append(String.format("Attempt %d: HTTP error; ", attempt));
                
                if (attempt < maxAttempts) {
                    logger.warn("HTTP request failed on attempt {}/{}, retrying in {} ms", 
                               attempt, maxAttempts, retryDelay);
                    Thread.sleep(retryDelay);
                }
                
            } catch (InterruptedException e) {
                logger.warn("HTTP retry interrupted on attempt {}", attempt);
                Thread.currentThread().interrupt();
                failureDetails.append(String.format("Attempt %d: Interrupted; ", attempt));
                break;
            } catch (IOException e) {
                String errorMsg = String.format("Attempt %d: %s; ", attempt, e.getMessage());
                failureDetails.append(errorMsg);
                
                logger.warn("HTTP request failed on attempt {}/{}: {}", attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                String errorMsg = String.format("Attempt %d: %s; ", attempt, e.getMessage());
                failureDetails.append(errorMsg);
                
                logger.warn("HTTP request failed on attempt {}/{}: {}", attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        String allFailures = failureDetails.toString();
        logger.error("HTTP request failed after {} attempts. Failure summary: {}", maxAttempts, allFailures);
        
        // Additional context for transaction rollback debugging
        if (config.isSessionTransacted()) {
            logger.error("JMS transaction will be rolled back due to HTTP callback failure. " +
                        "Message will be redelivered. Endpoint: {}", url);
        }
        
        return false;
    }
    
    /**
     * Send single HTTP request
     */
    private boolean sendHttpRequest(String url, String jsonPayload) throws IOException, InterruptedException {
        HttpRequest request = createHttpRequest(url, jsonPayload);
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            
            if (statusCode >= 200 && statusCode < 300) {
                logger.debug("HTTP request successful: {}", statusCode);
                return true;
            } else {
                logger.warn("HTTP request failed: {} - {}", statusCode, response.body());
                return false;
            }
        } catch (IOException | InterruptedException e) {
            logger.error("HTTP request exception: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Create HTTP request with configured method and headers
     */
    private HttpRequest createHttpRequest(String url, String jsonPayload) {
        String method = config.getHttpCallbackMethod().toUpperCase();
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(config.getHttpCallbackSocketTimeout()))
                .header("Content-Type", config.getHttpCallbackContentType())
                .header("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        
        // Add custom headers
        String authorization = config.getHttpCallbackHeader("authorization");
        if (authorization != null && !authorization.trim().isEmpty()) {
            requestBuilder.header("Authorization", authorization);
        }
        
        String xSource = config.getHttpCallbackHeader("x-source");
        if (xSource != null && !xSource.trim().isEmpty()) {
            requestBuilder.header("X-Source", xSource);
        }
        
        // Set HTTP method and body
        switch (method) {
            case "POST":
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonPayload));
                break;
            case "PUT":
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(jsonPayload));
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        
        return requestBuilder.build();
    }
    
    /**
     * Close HTTP client resources
     */
    public void close() {
        // Java's HttpClient doesn't require explicit closing
        // It manages its own resources automatically
        logger.info("HTTP client resources cleaned up");
    }
}
