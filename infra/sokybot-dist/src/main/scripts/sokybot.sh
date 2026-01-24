#!/bin/bash
# ============================================================================
# Sokybot Desktop Launcher for Linux/macOS
# ============================================================================
# This script launches the Sokybot application with bundled or system JRE.
#
# Environment Variables:
#   JAVA_HOME     - Path to JRE/JDK (optional if bundled JRE exists)
#   SOKYBOT_OPTS  - Additional JVM options
#   SOKYBOT_DEBUG - Set to "true" to enable debug output
# ============================================================================

set -e

APP_NAME="${app.name}"
APP_VERSION="${project.version}"

# Determine the application home directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$SCRIPT_DIR"

echo "Starting $APP_NAME v$APP_VERSION..."

# Function to find Java
find_java() {
    # Check for bundled JRE first
    if [ -x "$APP_HOME/jre/bin/java" ]; then
        JAVA_CMD="$APP_HOME/jre/bin/java"
        [ -n "$SOKYBOT_DEBUG" ] && echo "Using bundled JRE: $JAVA_CMD"
        return 0
    fi
    
    # Check JAVA_HOME
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        JAVA_CMD="$JAVA_HOME/bin/java"
        [ -n "$SOKYBOT_DEBUG" ] && echo "Using JAVA_HOME: $JAVA_CMD"
        return 0
    fi
    
    # Try to find java in PATH
    if command -v java &> /dev/null; then
        JAVA_CMD="java"
        [ -n "$SOKYBOT_DEBUG" ] && echo "Using java from PATH"
        return 0
    fi
    
    # On macOS, try the java_home utility
    if [ "$(uname)" = "Darwin" ]; then
        if [ -x "/usr/libexec/java_home" ]; then
            JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || true)
            if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
                JAVA_CMD="$JAVA_HOME/bin/java"
                [ -n "$SOKYBOT_DEBUG" ] && echo "Using macOS java_home: $JAVA_CMD"
                return 0
            fi
        fi
    fi
    
    return 1
}

# Find Java
if ! find_java; then
    echo "ERROR: No JRE found!"
    echo "Please either:"
    echo "  1. Bundle a JRE in the 'jre' folder"
    echo "  2. Set JAVA_HOME environment variable"
    echo "  3. Add java to your PATH"
    exit 1
fi

# Verify Java version
JAVA_VERSION=$("$JAVA_CMD" -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "WARNING: Java 11 or higher is recommended. Found: $JAVA_VERSION"
fi

# Set Karaf home
KARAF_HOME="$APP_HOME/runtime"

# Set default JVM options
DEFAULT_OPTS="-Xms256m -Xmx1024m"
DEFAULT_OPTS="$DEFAULT_OPTS -Dkaraf.home=$KARAF_HOME"
DEFAULT_OPTS="$DEFAULT_OPTS -Dkaraf.base=$KARAF_HOME"
DEFAULT_OPTS="$DEFAULT_OPTS -Dkaraf.data=$KARAF_HOME/data"
DEFAULT_OPTS="$DEFAULT_OPTS -Dkaraf.etc=$KARAF_HOME/etc"
DEFAULT_OPTS="$DEFAULT_OPTS -Dkaraf.instances=$KARAF_HOME/instances"
DEFAULT_OPTS="$DEFAULT_OPTS -Dkaraf.startLocalConsole=false"
DEFAULT_OPTS="$DEFAULT_OPTS -Dkaraf.startRemoteShell=false"
DEFAULT_OPTS="$DEFAULT_OPTS -Djava.util.logging.config.file=$KARAF_HOME/etc/java.util.logging.properties"

# macOS specific options
if [ "$(uname)" = "Darwin" ]; then
    DEFAULT_OPTS="$DEFAULT_OPTS -Xdock:name=$APP_NAME"
    # Uncomment below if you have an icon
    # DEFAULT_OPTS="$DEFAULT_OPTS -Xdock:icon=$APP_HOME/resources/icon.icns"
fi

# Combine with user options
JAVA_OPTS="$DEFAULT_OPTS ${SOKYBOT_OPTS:-}"

# Build classpath
CLASSPATH="$KARAF_HOME/system/org/apache/karaf/org.apache.karaf.main/${karaf.version}/org.apache.karaf.main-${karaf.version}.jar"
CLASSPATH="$CLASSPATH:$KARAF_HOME/system/org/apache/karaf/org.apache.karaf.specs.activator/${karaf.version}/org.apache.karaf.specs.activator-${karaf.version}.jar"

# Display debug info
if [ -n "$SOKYBOT_DEBUG" ]; then
    echo ""
    echo "Debug Information:"
    echo "  APP_HOME:   $APP_HOME"
    echo "  KARAF_HOME: $KARAF_HOME"
    echo "  JAVA_CMD:   $JAVA_CMD"
    echo "  JAVA_OPTS:  $JAVA_OPTS"
    echo ""
fi

# Launch the application
cd "$KARAF_HOME"
exec "$JAVA_CMD" $JAVA_OPTS -cp "$CLASSPATH" ${app.main.class}
