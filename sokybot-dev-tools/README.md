# Sokybot Dev Tools

A dedicated OSGi bundle providing development tools with a standalone React application for managing bundles, inspecting services, monitoring runtime metrics, and viewing logs.

## Quick Start

1. **Build the bundle** (automatically copies to `plugins/` directory):
   ```bash
   cd sokybot-dev-tools
   ./dev-build.sh
   ```
   
   The bundle will be automatically copied to `../plugins/` directory where FileInstall can detect it.

2. **FileInstall will automatically detect and install the bundle** if it's running and watching the `plugins/` directory.

3. **Access the UI**: Open `http://localhost:7001/devtools` in your browser

The UI will automatically connect to the RSocket server on port 7002.

**Note**: The Maven build is configured to automatically copy the bundle to the OSGi plugins directory (`../plugins/` relative to the bundle project). This directory is watched by FileInstall for hot-reload during development.

## Features

- **Bundle Management**: View, start, stop, restart, and reload OSGi bundles
- **Service Inspection**: Browse and inspect OSGi services by interface
- **Runtime Metrics**: Real-time monitoring of memory, threads, and JVM statistics
- **Log Viewer**: (Planned) View application logs in real-time
- **Hot Reload**: Automatic bundle reloading when files change (via FileInstall)
- **Event Streaming**: Real-time updates via RSocket streams

## Architecture

### Backend (Java)
- **DevToolsActivator**: Main bundle activator that wires everything together
- **BundleManagementService**: Manages OSGi bundle operations
- **ServiceInspectionService**: Inspects OSGi services
- **RuntimeMetricsService**: Collects JVM runtime metrics
- **DevToolsRSocketHandler**: Handles RSocket communication (port 7002)
- **DevToolsHttpServer**: Serves the React app (port 7001)
- **FileInstallListener**: Monitors FileInstall events
- **BundleStateListener**: Monitors bundle state changes

### Frontend (React + TypeScript)
- **React App**: Standalone application with tabbed interface
- **RSocket Client**: Connects to backend RSocket server
- **Views**: Bundles, Services, Metrics, Logs
- **UI Components**: Built with Radix UI and Tailwind CSS

## Verification

Before building, you can verify the setup:

```bash
cd sokybot-dev-tools
./dev-verify.sh
```

This will check:
- Maven availability
- Required files and directories
- Compilation
- Port availability

## Building

### Full Build (Java + Frontend)
```bash
cd sokybot-dev-tools
./dev-build.sh
```

Or using Maven:
```bash
mvn clean install
```

This will:
1. Install Node.js and npm (if not already installed)
2. Install frontend dependencies
3. Build the React app
4. Package the bundle with embedded frontend

### Frontend Only (Development)
```bash
cd sokybot-dev-tools
./dev-frontend-build.sh
```

Or manually:
```bash
cd src/main/frontend
npm install
npm run build
```

### Frontend Development Server
For hot-reload during development:
```bash
cd sokybot-dev-tools
./dev-watch.sh
```

## Usage

### Accessing the Dev Tools UI

Once the bundle is installed and started:
1. Open your browser
2. Navigate to: `http://localhost:7001/devtools`
3. The UI will automatically connect to the RSocket server on port 7002

### Bundle Management

- **View Bundles**: Lists all installed OSGi bundles with their states
- **Start/Stop**: Control bundle lifecycle
- **Restart**: Stop and start a bundle
- **Reload**: Reload a bundle from the filesystem (for hot-reload during development)
- **Search**: Filter bundles by name or location

### Service Inspection

- **List Services**: View all registered OSGi services
- **Filter by Interface**: Search for services implementing a specific interface
- **View Properties**: Expand services to see their properties
- **Bundle Association**: See which bundle provides each service

### Runtime Metrics

- **Memory**: Heap and non-heap memory usage with visual indicators
- **Threads**: Thread pool statistics
- **Runtime**: JVM information including uptime and processors
- **Auto-refresh**: Real-time updates every 2 seconds (can be toggled)

### Logs

Log viewer is planned for a future update. It will support:
- Real-time log streaming
- Level filtering
- Bundle filtering
- Search functionality
- Export capabilities

## Configuration

### Ports

- **HTTP Server**: 7001 (serves React app)
- **RSocket Server**: 7002 (handles API requests and streams)

These can be modified in `DevToolsActivator.java`.

### FileInstall Integration

The bundle automatically listens for FileInstall events if FileInstall is configured to watch bundle directories. This enables hot-reload when bundles are updated.

## Development

### Project Structure

```
sokybot-dev-tools/
├── src/
│   ├── main/
│   │   ├── java/org/sokybot/devtools/
│   │   │   ├── DevToolsActivator.java
│   │   │   ├── BundleManagementService.java
│   │   │   ├── ServiceInspectionService.java
│   │   │   ├── RuntimeMetricsService.java
│   │   │   ├── DevToolsRSocketHandler.java
│   │   │   ├── DevToolsHttpServer.java
│   │   │   ├── FileInstallListener.java
│   │   │   └── BundleStateListener.java
│   │   ├── frontend/
│   │   │   ├── src/
│   │   │   │   ├── App.tsx
│   │   │   │   ├── main.tsx
│   │   │   │   ├── lib/rsocket-client.ts
│   │   │   │   ├── services/devtools-service.ts
│   │   │   │   └── views/
│   │   │   └── package.json
│   │   └── resources/
│   └── target/
│       └── webapp/  # Built React app (after build)
├── pom.xml
└── dev-build.sh
```

### Dependencies

#### Backend
- RSocket Core & Transport
- Netty HTTP Server
- Jackson for JSON
- OSGi Framework & Services

#### Frontend
- React 19
- RSocket WebSocket Client
- Radix UI Components
- Tailwind CSS
- TypeScript

## Troubleshooting

### UI Not Loading

1. Ensure the bundle is installed and started
2. Check that ports 7001 and 7002 are not in use
3. Verify the webapp directory exists in `target/webapp` after build
4. Check logs for webapp path resolution issues

### RSocket Connection Failed

1. Verify RSocket server is running on port 7002
2. Check firewall settings
3. Ensure the bundle activated successfully
4. Check browser console for connection errors

### Bundles Not Showing

1. Verify bundle context is available
2. Check OSGi framework is running
3. Review logs for bundle management errors

## License

Same as parent Sokybot project.
