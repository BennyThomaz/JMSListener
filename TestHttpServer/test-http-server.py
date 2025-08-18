import json
import time
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
import urllib.parse

class JMSMessageHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        try:
            # Get content length
            content_length = int(self.headers.get('Content-Length', 0))
            
            # Read the POST data
            post_data = self.rfile.read(content_length)
            
            # Parse JSON
            try:
                message_data = json.loads(post_data.decode('utf-8'))
                
                # Log the received message
                print(f"\n{'='*50}")
                print(f"HTTP Callback Received at {datetime.now()}")
                print(f"{'='*50}")
                print(f"Message ID: {message_data.get('messageId', 'N/A')}")
                print(f"Message Type: {message_data.get('messageType', 'N/A')}")
                print(f"Content Type: {message_data.get('contentType', 'N/A')}")
                print(f"Content: {message_data.get('content', 'N/A')}")
                
                if message_data.get('correlationId'):
                    print(f"Correlation ID: {message_data['correlationId']}")
                
                if message_data.get('properties'):
                    print("Properties:")
                    for key, value in message_data['properties'].items():
                        print(f"  {key}: {value}")
                
                print(f"JMS Timestamp: {message_data.get('jmsTimestamp', 'N/A')}")
                print(f"Priority: {message_data.get('priority', 'N/A')}")
                print(f"Delivery Mode: {message_data.get('deliveryMode', 'N/A')}")
                print(f"{'='*50}")
                
                # Send success response
                self.send_response(200)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                
                response = {
                    "status": "success",
                    "message": "Message processed successfully",
                    "messageId": message_data.get('messageId'),
                    "timestamp": int(time.time() * 1000)
                }
                
                self.wfile.write(json.dumps(response).encode('utf-8'))
                
            except json.JSONDecodeError as e:
                print(f"ERROR: Invalid JSON received: {e}")
                self.send_error(400, "Invalid JSON")
                
        except Exception as e:
            print(f"ERROR: Exception processing request: {e}")
            self.send_error(500, "Internal server error")
    
    def do_PUT(self):
        # Handle PUT requests the same way as POST
        self.do_POST()
    
    def log_message(self, format, *args):
        # Custom logging to reduce noise
        print(f"[{datetime.now().strftime('%H:%M:%S')}] {format % args}")

def run_test_server(port=8080):
    server_address = ('', port)
    httpd = HTTPServer(server_address, JMSMessageHandler)
    
    print(f"JMS HTTP Callback Test Server")
    print(f"{'='*40}")
    print(f"Starting server on port {port}")
    print(f"URL: http://localhost:{port}/api/jms-messages")
    print(f"Press Ctrl+C to stop")
    print(f"{'='*40}")
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down server...")
        httpd.shutdown()

if __name__ == "__main__":
    # You can change the port here if needed
    run_test_server(8080)
