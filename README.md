# Sokybot

**Sokybot** is a modular, event-driven bot for Silkroad Online, built with Java (Karaf/OSGi) and Electron (React/TypeScript).

## 🚀 Getting Started

### Prerequisites
- **Java 11+**
- **Node.js 18+**
- **Maven 3.9+** (Wrapper included)

### 1. Installation

We provide a CLI tool `soky` to manage the entire development lifecycle.

1.  Make the script executable (if needed):
    ```bash
    chmod +x infra/scripts/soky
    ```

2.  Install the CLI globally:
    ```bash
    ./infra/scripts/soky install
    ```
    *Note: Ensure `~/bin` is in your `$PATH`.*

### 2. Building the Project

Use the `soky` CLI to build safely:

```bash
# Build everything (Backend + Frontend)
soky build all

# Build Backend only (Fast, skips tests)
soky build backend --fast

# Build UI only
soky build ui
```

### 3. Running the Application

You typically run the Backend and Frontend separately during development.

**Backend (Karaf OSGi Container)**
```bash
# Start in background
soky backend start

# View logs
soky backend logs

# Attach to shell (OSGi console)
soky backend shell
```

**Frontend (Webview + DevTools)**
```bash
soky ui dev
```
*Access Webview at http://localhost:5173 and DevTools at http://localhost:3000.*

### 4. Utilities

```bash
# Check status of all services
soky status

# Clean OSGi cache (fix weird errors)
soky backend clean

# Kill everything (Emergency)
soky kill
```

## 🏗 Architecture

- **Core**: Apache Karaf (OSGi)
- **Network**: Netty (Silkroad Protocol)
- **Database**: H2 (Embedded)
- **UI**: Electron + React
