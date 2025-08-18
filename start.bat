@echo off
REM JMS Listener Application Startup Script for Windows

REM Set application home
set APP_HOME=%~dp0
echo Application Home: %APP_HOME%

REM Set Java options
set JAVA_OPTS=-Xms512m -Xmx1024m
set JAVA_OPTS=%JAVA_OPTS% -Djava.awt.headless=true
set JAVA_OPTS=%JAVA_OPTS% -Dfile.encoding=UTF-8
set JAVA_OPTS=%JAVA_OPTS% -Duser.timezone=UTC
set JAVA_OPTS=%JAVA_OPTS% -Dweblogic.jndi.WLContext=DEBUG
set JAVA_OPTS=%JAVA_OPTS% -Dweblogic.log.DebugJMS=true


REM Set classpath
set CLASSPATH=%APP_HOME%target\jms-listener-1.0.0-jar-with-dependencies.jar
set CLASSPATH=%CLASSPATH%;%APP_HOME%lib\wlthint3client.jar

REM Configuration file
set CONFIG_FILE=%APP_HOME%src\main\resources\application.properties
if not exist "%CONFIG_FILE%" (
    echo Configuration file not found: %CONFIG_FILE%
    exit /b 1
)

REM Check for WebLogic client JAR
set WL_CLIENT_JAR=%APP_HOME%lib\wlthint3client.jar
if not exist "%WL_CLIENT_JAR%" (
    echo WebLogic client JAR not found: %WL_CLIENT_JAR%
    echo Please run setup-weblogic.bat first
    exit /b 1
)

REM Create logs directory if it doesn't exist
if not exist "%APP_HOME%logs" mkdir "%APP_HOME%logs"

echo Starting JMS Listener Application...
echo Java Options: %JAVA_OPTS%
echo Configuration: %CONFIG_FILE%

REM Start the application
java %JAVA_OPTS% -cp "%CLASSPATH%" com.example.jms.JMSListenerApplication "%CONFIG_FILE%"
