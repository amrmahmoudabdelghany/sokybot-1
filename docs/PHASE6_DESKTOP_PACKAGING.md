# Phase 6: Desktop Packaging Support

This document summarizes the desktop packaging improvements for Electron deployment.

## Overview

Phase 6 adds support for creating self-contained desktop distributions suitable for Electron wrapping:

1. **Platform-specific profiles** - Windows, macOS, Linux auto-detection
2. **Desktop distribution profile** - Creates bundled distribution
3. **Launch scripts** - Platform-appropriate launchers
4. **JRE bundling support** - Instructions and placeholders for bundled JRE

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Electron Application                     │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    Web UI (HTML/JS)                    │  │
│  └──────────────────────┬────────────────────────────────┘  │
│                         │ HTTP / WebSocket                   │
│  ┌──────────────────────▼────────────────────────────────┐  │
│  │              Sokybot Desktop Bundle                    │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐  │  │
│  │  │ Launch      │  │   Karaf     │  │  Bundled     │  │  │
│  │  │ Scripts     │→ │   Runtime   │  │  JRE (opt)   │  │  │
│  │  └─────────────┘  └─────────────┘  └──────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## New Profiles

### Platform Detection Profiles

These profiles automatically activate based on the build OS:

```xml
<!-- Activated automatically on Windows -->
<profile>
    <id>windows</id>
    <activation>
        <os><family>windows</family></os>
    </activation>
    <properties>
        <os.classifier>windows</os.classifier>
        <jre.platform>windows-x64</jre.platform>
    </properties>
</profile>

<!-- Activated automatically on macOS -->
<profile>
    <id>mac</id>
    <activation>
        <os><family>mac</family></os>
    </activation>
    <properties>
        <os.classifier>mac</os.classifier>
        <jre.platform>macos-x64</jre.platform>
    </properties>
</profile>

<!-- Activated automatically on Linux -->
<profile>
    <id>linux</id>
    <activation>
        <os><family>unix</family><name>Linux</name></os>
    </activation>
    <properties>
        <os.classifier>linux</os.classifier>
        <jre.platform>linux-x64</jre.platform>
    </properties>
</profile>
```

### Desktop Distribution Profile

```bash
./mvnw clean install -P desktop
```

This profile:
- Creates minimal Karaf assembly (no server consoles)
- Packages everything into a distributable bundle
- Includes launch scripts for all platforms
- Prepares JRE placeholder folder

## Output Structure

After building with `-P desktop`:

```
sokybot-dist/target/
└── Sokybot-1.0-SNAPSHOT-desktop/
    └── Sokybot-1.0-SNAPSHOT/
        ├── sokybot.bat        # Windows CMD launcher
        ├── sokybot.ps1        # Windows PowerShell launcher
        ├── sokybot.sh         # Linux/macOS launcher
        ├── README.txt         # User documentation
        ├── jre/               # JRE goes here (manual step)
        │   └── README-JRE.txt # JRE bundling instructions
        └── runtime/           # Karaf + application
            ├── bin/
            ├── data/
            ├── deploy/
            ├── etc/
            └── system/
```

## Launch Scripts

### Windows (`sokybot.bat`)
- Checks for bundled JRE in `jre/` folder
- Falls back to `JAVA_HOME` or `PATH`
- Supports `SOKYBOT_OPTS` for JVM options
- Debug mode with `SOKYBOT_DEBUG=true`

### Windows PowerShell (`sokybot.ps1`)
- Same functionality as batch script
- Better error messages and output formatting
- Recommended for advanced users

### Linux/macOS (`sokybot.sh`)
- POSIX-compliant shell script
- macOS-specific options (dock name)
- Supports bundled JRE, JAVA_HOME, or PATH

## JRE Bundling

### Option 1: Full JRE (Simpler)

1. Download JRE from https://adoptium.net/
2. Extract to `jre/` folder in distribution
3. Run launcher - it auto-detects bundled JRE

### Option 2: Custom JRE with jlink (Smaller)

```bash
# Identify required modules
jdeps --multi-release 11 --print-module-deps runtime/system/**/*.jar

# Create custom JRE (~40-60MB vs ~200MB)
jlink --add-modules java.base,java.logging,java.sql,java.naming,java.xml \
      --output jre \
      --strip-debug \
      --no-man-pages \
      --compress=2
```

## Usage

### Build Desktop Distribution

```bash
# Build desktop bundle
./mvnw clean install -P desktop -pl sokybot-dist -am

# Output location
ls sokybot-dist/target/Sokybot-*-desktop.zip
```

### Electron Integration

1. Build the desktop distribution
2. Copy to Electron project's resources
3. Launch via Electron's main process:

```javascript
// Electron main process
const { spawn } = require('child_process');
const path = require('path');

function launchBackend() {
    const isWindows = process.platform === 'win32';
    const script = isWindows ? 'sokybot.bat' : 'sokybot.sh';
    const backendPath = path.join(__dirname, 'backend', script);
    
    const backend = spawn(backendPath, [], {
        cwd: path.join(__dirname, 'backend'),
        stdio: 'inherit'
    });
    
    backend.on('error', (err) => {
        console.error('Failed to start backend:', err);
    });
    
    return backend;
}
```

### Development Workflow

```bash
# Development (with console, SSH)
./mvnw clean install -P dev

# Desktop testing
./mvnw clean install -P desktop
cd sokybot-dist/target/Sokybot-*/
./sokybot.sh  # or sokybot.bat on Windows

# Production release
./mvnw clean install -P release,desktop
```

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `JAVA_HOME` | Path to JRE (if not bundled) | `C:\Program Files\Java\jdk-11` |
| `SOKYBOT_OPTS` | Additional JVM options | `-Xmx2g -Dhttp.port=9090` |
| `SOKYBOT_DEBUG` | Enable debug output | `true` |

## Configuration

Default JVM settings in launch scripts:
- `-Xms256m` - Initial heap size
- `-Xmx1024m` - Maximum heap size
- Karaf console disabled (desktop mode)
- Remote shell disabled (security)

Override via `SOKYBOT_OPTS`:
```bash
export SOKYBOT_OPTS="-Xmx2g -Dhttp.port=9090"
./sokybot.sh
```

## Packaging for Distribution

### Windows (NSIS/Inno Setup)

1. Build desktop distribution
2. Bundle JRE
3. Create installer with NSIS or Inno Setup
4. Consider code signing for SmartScreen

### macOS (DMG)

1. Build desktop distribution
2. Bundle JRE (or use jlink for smaller size)
3. Create .app bundle with appropriate Info.plist
4. Sign and notarize for Gatekeeper

### Linux (AppImage/DEB/RPM)

1. Build desktop distribution
2. Bundle JRE
3. Create AppImage for universal distribution
4. Or create DEB/RPM for package managers

## Files Added

| File | Purpose |
|------|---------|
| `sokybot-dist/src/main/assembly/desktop-bundle.xml` | Assembly descriptor |
| `sokybot-dist/src/main/scripts/sokybot.bat` | Windows launcher |
| `sokybot-dist/src/main/scripts/sokybot.sh` | Unix launcher |
| `sokybot-dist/src/main/scripts/sokybot.ps1` | PowerShell launcher |
| `sokybot-dist/src/main/resources/dist/README.txt` | User documentation |
| `sokybot-dist/src/main/resources/jre-placeholder/README-JRE.txt` | JRE instructions |
