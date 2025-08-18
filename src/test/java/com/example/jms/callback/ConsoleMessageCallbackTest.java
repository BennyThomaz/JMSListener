package com.example.jms.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConsoleMessageCallback
 */
class ConsoleMessageCallbackTest {
    
    @Mock
    private TextMessage textMessage;
    
    private ConsoleMessageCallback callback;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        callback = new ConsoleMessageCallback();
        
        // Capture console output
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }
    
    @Test
    void testOnMessageWithTextMessage() throws JMSException {
        // Arrange
        String messageText = "Test message content";
        String messageId = "MSG-123";
        
        when(textMessage.getJMSMessageID()).thenReturn(messageId);
        when(textMessage.getText()).thenReturn(messageText);
        when(textMessage.getJMSTimestamp()).thenReturn(System.currentTimeMillis());
        
        // Act
        callback.onMessage(textMessage);
        
        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("JMS Message Received"));
        assertTrue(output.contains(messageId));
        assertTrue(output.contains(messageText));
        
        // Verify message methods were called
        verify(textMessage).getJMSMessageID();
        verify(textMessage).getText();
    }
    
    @Test
    void testOnMessageWithJMSException() throws JMSException {
        // Arrange
        when(textMessage.getJMSMessageID()).thenThrow(new JMSException("Test exception"));
        
        // Act & Assert
        assertThrows(JMSException.class, () -> callback.onMessage(textMessage));
    }
    
    void tearDown() {
        // Restore original System.out
        System.setOut(originalOut);
    }
}
