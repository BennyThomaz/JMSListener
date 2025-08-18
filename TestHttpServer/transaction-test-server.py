#!/usr/bin/env python3
"""
Transaction Rollback Test Server

This HTTP server simulates various failure scenarios to test JMS transaction rollback behavior.
It can be configured to fail requests at a specific rate to demonstrate automatic rollback.
"""

from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import time
import random
import argparse
import sys
from datetime import datetime

class TransactionTestHandler(BaseHTTPRequestHandler):
    
    def __init__(self, *args, fail_rate=0.0, delay_ms=0, **kwargs):
        self.fail_rate = fail_rate
        self.delay_ms = delay_ms
        self.request_count = 0
        super().__init__(*args, **kwargs)
    
    def do_POST(self):
        self.request_count += 1
        
        # Add artificial delay if configured
        if self.delay_ms > 0:
            time.sleep(self.delay_ms / 1000.0)
        
        # Determine if this request should fail
        should_fail = random.random() < self.fail_rate
        
        try:
            # Read request body
            content_length = int(self.headers.get('Content-Length', 0))
            request_body = self.rfile.read(content_length).decode('utf-8')
            
            # Parse JSON if possible
            try:
                message_data = json.loads(request_body)
                message_id = message_data.get('messageId', 'unknown')
            except json.JSONDecodeError:
                message_id = 'unknown'
            
            timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]
            
            if should_fail:
                # Simulate various types of failures
                failure_types = [
                    (500, "Internal Server Error"),
                    (503, "Service Unavailable"), 
                    (502, "Bad Gateway"),
                    (504, "Gateway Timeout")
                ]
                status_code, status_message = random.choice(failure_types)
                
                print(f"[{timestamp}] FAILURE {self.request_count}: {status_code} {status_message} for message {message_id}")
                
                self.send_response(status_code)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                
                error_response = {
                    "error": status_message,
                    "timestamp": timestamp,
                    "messageId": message_id,
                    "requestCount": self.request_count
                }
                
                self.wfile.write(json.dumps(error_response).encode('utf-8'))
                
            else:
                # Success response
                print(f"[{timestamp}] SUCCESS {self.request_count}: Message {message_id} processed successfully")
                
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                
                success_response = {
                    "status": "success",
                    "timestamp": timestamp,
                    "messageId": message_id,
                    "requestCount": self.request_count,
                    "message": "Message processed successfully"
                }
                
                self.wfile.write(json.dumps(success_response).encode('utf-8'))
        
        except Exception as e:
            print(f"[{timestamp}] ERROR {self.request_count}: Exception processing request: {e}")
            self.send_response(500)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            error_response = {
                "error": f"Server exception: {str(e)}",
                "timestamp": timestamp,
                "requestCount": self.request_count
            }
            
            self.wfile.write(json.dumps(error_response).encode('utf-8'))
    
    def do_PUT(self):
        # Delegate PUT requests to POST handler
        self.do_POST()
    
    def log_message(self, format, *args):
        # Suppress default HTTP server logging
        pass

def create_handler(fail_rate, delay_ms):
    def handler(*args, **kwargs):
        return TransactionTestHandler(*args, fail_rate=fail_rate, delay_ms=delay_ms, **kwargs)
    return handler

def main():
    parser = argparse.ArgumentParser(description='JMS Transaction Rollback Test Server')
    parser.add_argument('--port', type=int, default=8080, help='Server port (default: 8080)')
    parser.add_argument('--fail-rate', type=float, default=0.0, 
                       help='Failure rate (0.0-1.0, default: 0.0 for no failures)')
    parser.add_argument('--delay-ms', type=int, default=0,
                       help='Artificial delay in milliseconds (default: 0)')
    parser.add_argument('--demo', action='store_true',
                       help='Run demo mode with 30%% failure rate and 500ms delay')
    
    args = parser.parse_args()
    
    if args.demo:
        args.fail_rate = 0.3
        args.delay_ms = 500
        print("Demo mode: 30% failure rate, 500ms delay")
    
    # Validate fail rate
    if not 0.0 <= args.fail_rate <= 1.0:
        print("Error: fail-rate must be between 0.0 and 1.0")
        sys.exit(1)
    
    handler = create_handler(args.fail_rate, args.delay_ms)
    
    try:
        httpd = HTTPServer(('localhost', args.port), handler)
        
        print(f"Transaction Rollback Test Server starting...")
        print(f"  Port: {args.port}")
        print(f"  Failure Rate: {args.fail_rate:.1%}")
        print(f"  Delay: {args.delay_ms}ms")
        print(f"  URL: http://localhost:{args.port}/api/jms-messages")
        print()
        print("This server will simulate HTTP callback failures to test JMS transaction rollback.")
        print("Configure your JMS listener with:")
        print(f"  http.callback.url=http://localhost:{args.port}/api/jms-messages")
        print("  jms.session.transacted=true")
        print()
        print("Press Ctrl+C to stop...")
        print()
        
        httpd.serve_forever()
        
    except KeyboardInterrupt:
        print("\nShutting down server...")
        httpd.shutdown()
        httpd.server_close()
        print("Server stopped.")

if __name__ == '__main__':
    main()
