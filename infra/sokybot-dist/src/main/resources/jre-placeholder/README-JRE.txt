================================================================================
                           JRE BUNDLING INSTRUCTIONS
================================================================================

This folder is a placeholder for a bundled Java Runtime Environment (JRE).

To create a self-contained distribution that doesn't require users to install
Java, copy a JRE into this folder.

RECOMMENDED JRE
---------------
Eclipse Temurin (formerly AdoptOpenJDK) - https://adoptium.net/
- Version: 11 LTS or 17 LTS
- Architecture: Match your target platform (x64, aarch64)

DOWNLOADING JRE
---------------
1. Go to https://adoptium.net/temurin/releases/
2. Select:
   - Operating System: Windows / Linux / macOS
   - Architecture: x64 (most common) or aarch64 (ARM)
   - Package Type: JRE
   - Version: 11 or 17

PLATFORM-SPECIFIC DOWNLOADS
---------------------------
Windows x64:
  https://adoptium.net/temurin/releases/?os=windows&arch=x64&package=jre

Linux x64:
  https://adoptium.net/temurin/releases/?os=linux&arch=x64&package=jre

macOS x64 (Intel):
  https://adoptium.net/temurin/releases/?os=mac&arch=x64&package=jre

macOS aarch64 (Apple Silicon):
  https://adoptium.net/temurin/releases/?os=mac&arch=aarch64&package=jre

INSTALLATION
------------
After downloading, extract the JRE and copy its contents to this folder.

Expected structure:
jre/
├── bin/
│   ├── java (or java.exe on Windows)
│   └── ...
├── conf/
├── legal/
├── lib/
└── release

USING JLINK (ADVANCED)
----------------------
For smaller distributions, use jlink to create a custom JRE:

# Identify required modules (run from project root after build)
jdeps --multi-release 11 --print-module-deps runtime/system/**/*.jar

# Create custom JRE
jlink --add-modules java.base,java.logging,java.sql,java.naming,java.management,java.xml \
      --output jre \
      --strip-debug \
      --no-man-pages \
      --no-header-files \
      --compress=2

This can reduce JRE size from ~200MB to ~40-60MB.

SIZE COMPARISON
---------------
Full JRE 11:    ~180-200 MB
Custom jlink:   ~40-60 MB (varies by modules)

================================================================================
