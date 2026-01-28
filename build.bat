@echo off
echo Setting up build environment...

REM Set JDK path
set JAVA_HOME=F:\Development\jdk-23.0.2
set PATH=%JAVA_HOME%\bin;%PATH%

REM Set Maven path
set MAVEN_HOME=F:\Development\apache-maven-3.9.11
set PATH=%MAVEN_HOME%\bin;%PATH%

echo JAVA_HOME: %JAVA_HOME%
echo MAVEN_HOME: %MAVEN_HOME%

echo.
echo Verifying Java installation...
java -version

echo.
echo Verifying Maven installation...
call mvn -version
echo Maven verification completed.

echo.
echo Starting Maven build...
call mvn clean compile -DskipTests
if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build completed successfully!
    echo.
    echo Creating package...
    call mvn package -DskipTests
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo Package created successfully!
        echo JAR file location: target\jms-listener-1.0.0-jar-with-dependencies.jar
        echo.
        echo Copying application.properties to target folder...
        copy "src\main\resources\application.properties" "target\" >nul
        if exist "target\application.properties" (
            echo application.properties copied successfully!
        ) else (
            echo Warning: Failed to copy application.properties
        )
        
        echo.
        echo Copying provider configuration files...
        if not exist "target\config" mkdir "target\config"
        copy "src\main\resources\config\*.json" "target\config\" >nul
        if exist "target\config\weblogic.provider.json" (
            echo Provider configuration files copied successfully!
            echo Available provider files:
            dir "target\config\*.json" /b
        ) else (
            echo Warning: Failed to copy provider configuration files
        )
    ) else (
        echo.
        echo Package creation failed!
    )
) else (
    echo.
    echo Build failed!
)

echo.
pause
