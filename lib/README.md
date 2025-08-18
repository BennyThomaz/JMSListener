# WebLogic Client JAR Setup

This directory should contain the WebLogic client JAR file required for connecting to WebLogic Server.

## Required File

Place the `wlthint3client.jar` file in this directory:
```
lib/wlthint3client.jar
```

## How to Obtain wlthint3client.jar

### Option 1: From WebLogic Server Installation

If you have WebLogic Server installed, you can find the client JAR at:
```
%WL_HOME%/server/lib/wlthint3client.jar
```

Copy this file to the `lib/` directory in your project.

### Option 2: Generate from WebLogic Installation

Navigate to your WebLogic server lib directory and run:
```cmd
cd %WL_HOME%/server/lib
java -jar wljarbuilder.jar
```

This will create `wlfullclient.jar`. You can either:
- Rename it to `wlthint3client.jar` and place it in the `lib/` directory
- Update the `pom.xml` to reference `wlfullclient.jar` instead

### Option 3: Download from Oracle

1. Go to Oracle's Maven Repository or download center
2. Download the appropriate WebLogic thin client
3. Place it in the `lib/` directory as `wlthint3client.jar`

## Alternative: Using Maven Install

If you prefer to install the JAR to your local Maven repository:

```cmd
mvn install:install-file -Dfile=path/to/wlthint3client.jar -DgroupId=com.oracle.weblogic -DartifactId=wlclient -Dversion=14.1.1 -Dpackaging=jar
```

Then update the `pom.xml` to use a regular dependency instead of system scope:

```xml
<dependency>
    <groupId>com.oracle.weblogic</groupId>
    <artifactId>wlclient</artifactId>
    <version>14.1.1</version>
</dependency>
```

## Current Status

⚠️ **Missing**: `wlthint3client.jar` is not yet present in this directory.

Place the WebLogic client JAR here to enable full WebLogic functionality.

## Temporary Workaround

For development and testing without WebLogic:
1. Comment out the WebLogic dependency in `pom.xml`
2. Use alternative JMS providers like ActiveMQ for testing
3. The application will compile and run with mock configurations
