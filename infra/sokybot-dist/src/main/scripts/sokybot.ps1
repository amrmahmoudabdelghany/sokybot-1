# ============================================================================
# Sokybot Desktop Launcher for Windows (PowerShell)
# ============================================================================
# This script launches the Sokybot application with bundled or system JRE.
#
# Environment Variables:
#   JAVA_HOME     - Path to JRE/JDK (optional if bundled JRE exists)
#   SOKYBOT_OPTS  - Additional JVM options
#   SOKYBOT_DEBUG - Set to "true" to enable debug output
# ============================================================================

$ErrorActionPreference = "Stop"

$AppName = "${app.name}"
$AppVersion = "${project.version}"
$KarafVersion = "${karaf.version}"
$MainClass = "${app.main.class}"

# Determine the application home directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$AppHome = $ScriptDir

Write-Host "Starting $AppName v$AppVersion..."

# Function to find Java
function Find-Java {
    # Check for bundled JRE first
    $bundledJava = Join-Path $AppHome "jre\bin\java.exe"
    if (Test-Path $bundledJava) {
        if ($env:SOKYBOT_DEBUG) { Write-Host "Using bundled JRE: $bundledJava" }
        return $bundledJava
    }
    
    # Check JAVA_HOME
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
        $javaCmd = Join-Path $env:JAVA_HOME "bin\java.exe"
        if ($env:SOKYBOT_DEBUG) { Write-Host "Using JAVA_HOME: $javaCmd" }
        return $javaCmd
    }
    
    # Try to find java in PATH
    $javaInPath = Get-Command java -ErrorAction SilentlyContinue
    if ($javaInPath) {
        if ($env:SOKYBOT_DEBUG) { Write-Host "Using java from PATH" }
        return "java"
    }
    
    return $null
}

# Find Java
$JavaCmd = Find-Java
if (-not $JavaCmd) {
    Write-Host "ERROR: No JRE found!" -ForegroundColor Red
    Write-Host "Please either:"
    Write-Host "  1. Bundle a JRE in the 'jre' folder"
    Write-Host "  2. Set JAVA_HOME environment variable"
    Write-Host "  3. Add java to your PATH"
    Read-Host "Press Enter to exit"
    exit 1
}

# Set Karaf home
$KarafHome = Join-Path $AppHome "runtime"

# Set default JVM options
$DefaultOpts = @(
    "-Xms256m",
    "-Xmx1024m",
    "-Dkaraf.home=$KarafHome",
    "-Dkaraf.base=$KarafHome",
    "-Dkaraf.data=$KarafHome\data",
    "-Dkaraf.etc=$KarafHome\etc",
    "-Dkaraf.instances=$KarafHome\instances",
    "-Dkaraf.startLocalConsole=false",
    "-Dkaraf.startRemoteShell=false",
    "-Djava.util.logging.config.file=$KarafHome\etc\java.util.logging.properties"
)

# Add user options
$JavaOpts = $DefaultOpts
if ($env:SOKYBOT_OPTS) {
    $JavaOpts += $env:SOKYBOT_OPTS -split ' '
}

# Build classpath
$Classpath = @(
    "$KarafHome\system\org\apache\karaf\org.apache.karaf.main\$KarafVersion\org.apache.karaf.main-$KarafVersion.jar",
    "$KarafHome\system\org\apache\karaf\org.apache.karaf.specs.activator\$KarafVersion\org.apache.karaf.specs.activator-$KarafVersion.jar"
) -join ";"

# Display debug info
if ($env:SOKYBOT_DEBUG) {
    Write-Host ""
    Write-Host "Debug Information:"
    Write-Host "  APP_HOME:   $AppHome"
    Write-Host "  KARAF_HOME: $KarafHome"
    Write-Host "  JAVA_CMD:   $JavaCmd"
    Write-Host "  JAVA_OPTS:  $($JavaOpts -join ' ')"
    Write-Host ""
}

# Launch the application
Set-Location $KarafHome
$process = Start-Process -FilePath $JavaCmd -ArgumentList ($JavaOpts + @("-cp", $Classpath, $MainClass)) -NoNewWindow -Wait -PassThru

# Return exit code
exit $process.ExitCode
