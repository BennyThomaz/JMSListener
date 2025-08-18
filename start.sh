#!/bin/bash

# JMS Listener Application Startup Script

# Set application home
APP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Application Home: $APP_HOME"

# Set Java options
JAVA_OPTS="-Xms512m -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"
JAVA_OPTS="$JAVA_OPTS -Duser.timezone=UTC"

# Set classpath
CLASSPATH="$APP_HOME/target/jms-listener-1.0.0-jar-with-dependencies.jar"

# Configuration file
CONFIG_FILE="$APP_HOME/src/main/resources/application.properties"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Configuration file not found: $CONFIG_FILE"
    exit 1
fi

# Create logs directory if it doesn't exist
mkdir -p "$APP_HOME/logs"

echo "Starting JMS Listener Application..."
echo "Java Options: $JAVA_OPTS"
echo "Configuration: $CONFIG_FILE"

# Start the application
java $JAVA_OPTS -cp "$CLASSPATH" com.example.jms.JMSListenerApplication "$CONFIG_FILE"
