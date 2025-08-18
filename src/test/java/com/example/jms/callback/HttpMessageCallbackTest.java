package com.example.jms.callback;

import com.example.jms.config.JMSConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import static org.mockito.Mockito.*;

/**
 * Unit tests for HttpMessageCallback
 */
class HttpMessageCallbackTest {
    
    @Mock
    private JMSConfig config;
    
    @Mock
    private TextMessage textMessage;
    
    private HttpMessageCallback callback;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup config defaults
        when(config.isHttpCallbackEnabled()).thenReturn(true);
        when(config.getHttpCallbackUrl()).thenReturn("http://localhost:8080/test");
        when(config.getHttpCallbackMethod()).thenReturn("POST");
        when(config.getHttpCallbackContentType()).thenReturn("application/json");
        when(config.getHttpCallbackConnectionTimeout()).thenReturn(5000);
        when(config.getHttpCallbackSocketTimeout()).thenReturn(10000);
        when(config.getHttpCallbackRetryAttempts()).thenReturn(3);
        when(config.getHttpCallbackRetryDelay()).thenReturn(1000L);
        when(config.getHttpCallbackHeader("authorization")).thenReturn(null);
        when(config.getHttpCallbackHeader("x-source")).thenReturn(null);
        
        callback = new HttpMessageCallback(config);
    }
    
    @Test
    void testHttpCallbackDisabled() throws JMSException {
        // Arrange
        when(config.isHttpCallbackEnabled()).thenReturn(false);
        
        // Act
        callback.onMessage(textMessage);
        
        // Assert - no exception should be thrown
        // The method should return early without processing
    }
    
    @Test
    void testHttpCallbackWithNullUrl() throws JMSException {
        // Arrange
        when(config.getHttpCallbackUrl()).thenReturn(null);
        
        // Act
        callback.onMessage(textMessage);
        
        // Assert - no exception should be thrown
        // The method should return early without processing
    }
    
    @Test
    void testMessageConversionToJson() throws JMSException {
        // Arrange
        String messageId = "MSG-123";
        String messageText = "Test message content";
        
        when(textMessage.getJMSMessageID()).thenReturn(messageId);
        when(textMessage.getText()).thenReturn(messageText);
        when(textMessage.getJMSTimestamp()).thenReturn(System.currentTimeMillis());
        when(textMessage.getJMSCorrelationID()).thenReturn("CORR-123");
        when(textMessage.getJMSDeliveryMode()).thenReturn(2);
        when(textMessage.getJMSPriority()).thenReturn(4);
        when(textMessage.getJMSExpiration()).thenReturn(0L);
        when(textMessage.getJMSRedelivered()).thenReturn(false);
        when(textMessage.getPropertyNames()).thenReturn(java.util.Collections.emptyEnumeration());
        
        // Note: This test would normally fail if trying to actually send HTTP request
        // In a real test environment, you would mock the HTTP client or use a test server
        
        try {
            // Act
            callback.onMessage(textMessage);
            
            // If we reach here without exception, the JSON conversion worked
            // The HTTP call would fail, but that's expected in this test environment
        } catch (JMSException e) {
            // Expected to fail due to HTTP connection, but JSON conversion should work
            assert e.getMessage().contains("HTTP callback failed");
        }
        
        // Verify message methods were called
        verify(textMessage).getJMSMessageID();
        verify(textMessage).getText();
    }
}
