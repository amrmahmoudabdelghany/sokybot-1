# Testing Strategy for Declarative UI Extension System

## Overview

This document outlines the comprehensive testing strategy for the declarative UI extension system, covering unit tests, integration tests, and end-to-end tests.

## Test Structure

```
sokybot-webview/
├── src/test/java/              # Backend Java tests
│   └── org/sokybot/webview/
│       ├── SchemaLoaderTest.java
│       ├── WebviewConfiguratorTest.java
│       └── RSocketIntegrationTest.java
└── src/main/frontend/
    └── src/
        ├── test/               # Frontend test setup
        │   └── setup.ts
        └── extensions/
            └── renderer/
                └── ComponentRenderer.test.tsx
```

## Backend Tests (Java)

### Unit Tests

#### 1. SchemaLoaderTest
**Purpose**: Test JSON schema loading and $ref resolution

**Test Cases**:
- ✅ Load schema from resources
- ✅ Return null for non-existent schema
- ✅ Resolve $ref references
- ✅ Handle schemas without children
- ✅ Handle nested schema structures

**Location**: `src/test/java/org/sokybot/webview/SchemaLoaderTest.java`

#### 2. WebviewConfiguratorTest
**Purpose**: Test the core configurator service

**Test Cases**:
- ✅ Register declarative page
- ✅ Register and invoke action handler
- ✅ Return error for unregistered action
- ✅ Register and invoke stream handler
- ✅ Return empty stream for unregistered stream
- ✅ Register schema handler
- ✅ Handle multiple pages independently
- ✅ Remove page and cleanup handlers
- ✅ Get extension registry

**Location**: `src/test/java/org/sokybot/webview/WebviewConfiguratorTest.java`

#### 3. PacketSnifferServiceTest
**Purpose**: Test packet sniffer service (example plugin)

**Test Cases**:
- ✅ Handle schema request
- ✅ Handle switchTab action
- ✅ Handle clearMonitor action
- ✅ Handle togglePause action
- ✅ Stream packets
- ✅ Stream statistics
- ✅ Handle filterTracer action
- ✅ Shutdown gracefully

**Location**: `sokybot-packet-sniffer/src/test/java/org/sokybot/packetsniffer/PacketSnifferServiceTest.java`

### Integration Tests

#### 1. RSocketIntegrationTest
**Purpose**: Test RSocket communication between backend and frontend

**Test Cases**:
- ✅ Handle schema request via RSocket
- ✅ Handle action request via RSocket
- ✅ Handle stream request via RSocket
- ✅ Send events to frontend
- ✅ Handle multiple concurrent requests

**Location**: `src/test/java/org/sokybot/webview/RSocketIntegrationTest.java`

## Frontend Tests (TypeScript/React)

### Unit Tests

#### 1. ComponentRenderer.test.tsx
**Purpose**: Test the component rendering engine

**Test Cases**:
- ✅ Render simple div component
- ✅ Resolve template expressions
- ✅ Handle nested components
- ✅ Handle conditional rendering with hidden prop
- ✅ Handle boolean expression in hidden prop
- ✅ Call onAction when button is clicked
- ✅ Resolve className with template

**Location**: `src/main/frontend/src/extensions/renderer/ComponentRenderer.test.tsx`

### Test Setup

**Vitest Configuration**: `vitest.config.ts`
- Uses jsdom environment for React testing
- Configures path aliases (@/ for src/)
- Sets up test globals

**Test Setup File**: `src/test/setup.ts`
- Configures @testing-library/jest-dom matchers
- Cleans up after each test

## Running Tests

### Backend Tests (Maven)

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=WebviewConfiguratorTest

# Run with coverage
mvn test jacoco:report
```

### Frontend Tests (npm)

```bash
cd sokybot-webview/src/main/frontend

# Run all tests
npm test

# Run in watch mode
npm test -- --watch

# Run with coverage
npm test -- --coverage
```

## Test Coverage Goals

### Backend
- **Unit Tests**: 80%+ coverage
- **Integration Tests**: All critical paths
- **Key Areas**:
  - Schema loading and resolution
  - Action handler registration and invocation
  - Stream handler registration and data flow
  - RSocket communication

### Frontend
- **Component Tests**: 70%+ coverage
- **Key Areas**:
  - Template expression resolution
  - Component rendering
  - Action handling
  - Stream subscription

## Integration Test Scenarios

### Scenario 1: Complete Plugin Lifecycle
1. Plugin registers page with schema
2. Frontend requests schema
3. User interacts with UI (button click)
4. Action handler processes request
5. Stream handler provides real-time updates
6. Plugin unregisters page

### Scenario 2: Multiple Plugins
1. Two plugins register pages simultaneously
2. Both pages appear in UI
3. Actions from both pages work independently
4. Streams from both plugins work independently

### Scenario 3: Error Handling
1. Invalid schema handling
2. Missing action handler
3. Stream errors
4. RSocket connection failures

## Performance Tests

### Backend Performance
- **Schema Loading**: < 10ms per schema
- **Action Handling**: < 50ms per action
- **Stream Throughput**: 1000+ messages/second

### Frontend Performance
- **Component Rendering**: < 16ms for 100 components
- **Template Resolution**: < 1ms per expression
- **Stream Processing**: < 5ms per message

## Continuous Integration

### Pre-commit Hooks
- Run unit tests
- Check code coverage thresholds
- Lint code

### CI Pipeline
1. Run all unit tests
2. Run integration tests
3. Build bundles
4. Run end-to-end tests
5. Generate coverage reports

## Test Data

### Mock Data
- Sample JSON schemas in `src/test/resources/ui/`
- Mock RSocket responses
- Mock packet data for packet sniffer tests

### Test Fixtures
- Common component schemas
- Action handler templates
- Stream handler templates

## Future Enhancements

1. **E2E Tests**: Playwright/Cypress tests for full user flows
2. **Visual Regression Tests**: Screenshot comparison for UI changes
3. **Load Tests**: Test system under high packet/event load
4. **Security Tests**: Test for XSS vulnerabilities in template expressions
5. **Accessibility Tests**: Ensure UI components are accessible

## Best Practices

1. **Test Isolation**: Each test should be independent
2. **Mock External Dependencies**: Use mocks for RSocket, file system, etc.
3. **Test Edge Cases**: Empty schemas, null values, malformed data
4. **Test Error Paths**: Invalid actions, missing handlers, connection failures
5. **Keep Tests Fast**: Unit tests should run in < 1 second
6. **Clear Test Names**: Use descriptive test names that explain what is being tested

## Troubleshooting

### Common Issues

1. **RSocket Connection**: Ensure RSocketServerService is properly mocked
2. **Schema Loading**: Check resource paths and classpath
3. **Template Resolution**: Verify context object structure
4. **Stream Tests**: Use appropriate timeouts for async operations

### Debug Tips

- Enable debug logging: `-Dorg.slf4j.simpleLogger.log.org.sokybot.webview=debug`
- Use React Testing Library's `screen.debug()` for component inspection
- Use Maven's `-X` flag for verbose test output
