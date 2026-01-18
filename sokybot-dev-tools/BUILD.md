# Build Instructions

This document provides detailed build instructions for the `sokybot-dev-tools` bundle.

## Prerequisites

- **Java**: JDK 11 or higher
- **Maven**: 3.6 or higher
- **Node.js**: v20.10.0 (automatically installed by Maven if not present)
- **npm**: 10.2.3 (automatically installed by Maven if not present)

## Quick Build

The simplest way to build the entire bundle:

```bash
cd sokybot-dev-tools
./dev-build.sh
```

This will:
1. Install Node.js and npm (if not already installed)
2. Install frontend dependencies
3. Build the React frontend
4. Compile Java sources
5. Package everything into a JAR bundle

The output will be in `target/sokybot-dev-tools-*.jar`

## Step-by-Step Build

### 1. Verify Setup

First, verify your environment:

```bash
./dev-verify.sh
```

### 2. Build Frontend

To build only the frontend:

```bash
./dev-frontend-build.sh
```

Or manually:

```bash
cd src/main/frontend
npm install
npm run build
```

The frontend will be built to `target/webapp/`

### 3. Build Bundle

To build only the Java bundle (assumes frontend is already built):

```bash
mvn clean package
```

Or to install to local Maven repository:

```bash
mvn clean install
```

## Development Workflow

### Frontend Development

For frontend development with hot-reload:

```bash
./dev-watch.sh
```

This starts the Vite dev server on `http://localhost:3000`. Note that in dev mode, you'll need to connect to the RSocket server on port 7002, but the HTTP server from the bundle won't be used.

### Backend Development

1. Build the frontend once:
   ```bash
   ./dev-frontend-build.sh
   ```

2. Build the bundle:
   ```bash
   mvn clean package
   ```

3. Copy the bundle JAR to your bundle directory (if using FileInstall):
   ```bash
   cp target/sokybot-dev-tools-*.jar /path/to/bundles/
   ```

4. The bundle will be automatically installed/updated by FileInstall.

### Hot Reload with FileInstall

If FileInstall is configured to watch your bundle `target/` directory:

1. Build the bundle:
   ```bash
   mvn clean package
   ```

2. FileInstall will automatically detect the new/updated bundle and install it.

3. The bundle will start and serve the UI at `http://localhost:7001/devtools`

## Build Configuration

### Maven Configuration

The `pom.xml` includes:

- **frontend-maven-plugin**: Handles Node.js/npm installation and frontend build
- **maven-bundle-plugin**: Packages the OSGi bundle with proper metadata
- **Embed-Dependency**: Includes RSocket, Netty, and Jackson dependencies

### Frontend Configuration

- **Vite**: Build tool (configured in `vite.config.ts`)
- **TypeScript**: Type checking (configured in `tsconfig.json`)
- **Tailwind CSS**: Styling (configured in `tailwind.config.js`)

### Output Structure

After build:

```
target/
├── webapp/              # Frontend build output
│   ├── index.html
│   ├── assets/
│   └── ...
└── sokybot-dev-tools-*.jar  # Final bundle (includes webapp/)
```

## Troubleshooting

### Frontend Build Fails

1. **Node.js/npm not found**: Maven will auto-install them. If that fails, install manually.
2. **Dependencies not installing**: Try deleting `node_modules/` and `package-lock.json`, then run `npm install` again.
3. **TypeScript errors**: Check `tsconfig.json` and ensure all imports are correct.

### Bundle Build Fails

1. **Frontend not built**: Ensure `target/webapp/` exists. Run `./dev-frontend-build.sh` first.
2. **OSGi manifest errors**: Check the `maven-bundle-plugin` configuration in `pom.xml`.
3. **Missing dependencies**: Verify all dependencies are declared in `pom.xml`.

### Bundle Won't Start

1. **Ports in use**: Ensure ports 7001 (HTTP) and 7002 (RSocket) are available.
2. **Webapp not found**: Check bundle logs for webapp path resolution. The bundle looks for `webapp/` in bundle resources.
3. **RSocket server fails**: Check logs for binding errors. Verify no firewall is blocking ports.

### Frontend Not Loading

1. **404 errors**: Ensure the webapp was built and included in the bundle.
2. **RSocket connection fails**: Verify the bundle started successfully and RSocket server is running on port 7002.
3. **CORS errors**: The HTTP server includes CORS headers. If issues persist, check browser console.

## Advanced

### Custom Ports

To change ports, edit `DevToolsActivator.java`:

```java
private static final int RSOCKET_PORT = 7002; // Change this
private static final int HTTP_PORT = 7001;    // Change this
```

### Development Without Maven Build

For rapid frontend iteration, you can:

1. Start the bundle with a development webapp path
2. Use Vite dev server with proxy to RSocket server
3. Configure Vite to proxy API requests

However, the recommended approach is to use `dev-watch.sh` for frontend development and rebuild the bundle for testing.

## CI/CD Integration

For automated builds:

```bash
# Install dependencies and build
mvn clean install -DskipTests

# The bundle JAR will be in target/
```

You can configure your CI/CD to:
1. Run tests (when added)
2. Build the bundle
3. Deploy to your bundle repository
