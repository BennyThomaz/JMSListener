package com.example.jms.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Simple network connectivity test utility
 */
public class NetworkTest {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java NetworkTest <host> <port> [timeout_ms]");
            System.out.println("Example: java NetworkTest 192.168.0.196 7001 10000");
            System.exit(1);
        }
        
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int timeoutMs = args.length > 2 ? Integer.parseInt(args[2]) : 10000;
        
        System.out.println("Testing network connectivity to " + host + ":" + port);
        System.out.println("Timeout: " + timeoutMs + "ms");
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("✅ SUCCESS: Connected to " + host + ":" + port);
            System.out.println("Connection time: " + duration + "ms");
            System.out.println("Socket info: " + socket.getRemoteSocketAddress());
            
        } catch (SocketTimeoutException e) {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("❌ TIMEOUT: Connection timed out after " + duration + "ms");
            System.out.println("Error: " + e.getMessage());
            
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("❌ ERROR: Connection failed after " + duration + "ms");
            System.out.println("Error: " + e.getMessage());
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("❌ UNEXPECTED ERROR after " + duration + "ms");
            System.out.println("Error: " + e.getMessage());
        }
        
        System.out.println();
        System.out.println("Network test completed.");
    }
}
