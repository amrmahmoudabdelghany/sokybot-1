# Sokybot Proxy Testing Framework

This directory contains the testing framework for the `sokybot-proxy` bundle. The framework provides utilities, base classes, and helpers to facilitate comprehensive testing of proxy components.

## Overview

The testing framework is designed to:
- Simplify creation of proxy connection tests
- Mock Netty channels and handlers
- Create test packets easily
- Track connection lifecycle events
- Provide common test infrastructure

## Structure

```
src/test/java/org/sokybot/proxy/
├── test/
│   └── ProxyTestBase.java          # Base class for all proxy tests
│   └── util/
│       ├── MockConnectionListener.java  # Mock listener for connection events
│       ├── NettyTestUtils.java          # Utilities for Netty mocking
│       └── PacketTestUtils.java         # Utilities for packet creation
├── ProxyConnectionTest.java        # Example: ProxyConnection tests
└── ProxyConnectionFactoryTest.java # Example: Factory tests
```

## Base Classes

### ProxyTestBase

All proxy tests should extend `ProxyTestBase`, which provides:

- **Mock Infrastructure**: Pre-configured mocks for `EventAdmin`
- **Connection Factory**: Ready-to-use `IProxyConnectionFactory` instance
- **Event Loop Groups**: Netty event loop groups for testing
- **Test Utilities**: Convenient access to `NettyTestUtils` and `PacketTestUtils`
- **Helper Methods**: Methods for creating connections, waiting for conditions, etc.

#### Example Usage

```java
@DisplayName("My Proxy Test")
class MyProxyTest extends ProxyTestBase {
    
    @Test
    void testSomething() {
        IProxyConnection connection = createConnection();
        // Test code here...
    }
}
```

## Utilities

### MockConnectionListener

A mock implementation of `IConnectionListener` that tracks all connection events and provides assertions.

**Features:**
- Tracks all connection events in order
- Provides countdown latches for async event waiting
- Stores event data (redirect info, server name, etc.)
- Assertion methods for verifying event sequences

#### Example Usage

```java
@Test
void testConnectionEvents() throws InterruptedException {
    MockConnectionListener listener = new MockConnectionListener();
    listener.expectClientConnected();
    listener.expectServerConnected();
    
    IProxyConnection connection = createConnection(TEST_MACHINE_ID, listener);
    
    // Trigger connections...
    
    listener.waitForClientConnected(5000);
    listener.waitForServerConnected(5000);
    
    assertTrue(listener.wasClientConnected());
    assertTrue(listener.wasServerConnected());
}
```

### PacketTestUtils

Utilities for creating test packets with various configurations.

**Methods:**
- `createPacket(opcode, data)` - Create a basic packet
- `createPacket(opcode, data, packetEncrypted, dataEncrypted)` - Create encrypted packet
- `createMutablePacket(opcode, data)` - Create mutable packet for sending
- `createPacketWithString(opcode, string)` - Create packet with string data
- `createPacketWithInt(opcode, intValue)` - Create packet with integer
- `createEmptyPacket(opcode)` - Create packet with only header

#### Example Usage

```java
@Test
void testPacketCreation() {
    PacketTestUtils utils = packetUtils();
    
    // Create a simple packet
    ImmutablePacket packet = utils.createPacket(0x2001, new byte[]{1, 2, 3});
    
    // Create an encrypted packet
    ImmutablePacket encrypted = utils.createPacket(0x2001, data, true, false);
    
    // Create packet with string
    ImmutablePacket withString = utils.createPacketWithString(0xA103, "test-string");
}
```

### NettyTestUtils

Utilities for creating Netty mocks for channels, contexts, and futures.

**Methods:**
- `createMockChannel()` - Create a mock Netty channel
- `createMockSucceededFuture(channel)` - Create successful channel future
- `createMockFailedFuture(channel, cause)` - Create failed channel future
- `createMockContext(channel)` - Create mock channel handler context
- `waitForChannelOperation(future, timeout)` - Wait for channel operation

#### Example Usage

```java
@Test
void testNettyMocks() {
    NettyTestUtils utils = nettyUtils();
    
    Channel mockChannel = utils.createMockChannel();
    ChannelHandlerContext ctx = utils.createMockContext(mockChannel);
    
    // Use mocks in tests...
}
```

## Writing Tests

### Basic Test Structure

1. Extend `ProxyTestBase`
2. Use `@DisplayName` for descriptive test names
3. Use helper methods from base class
4. Use utilities for creating test data

```java
@DisplayName("My Component Tests")
class MyComponentTest extends ProxyTestBase {
    
    @Test
    @DisplayName("Should do something")
    void testSomething() {
        // Arrange
        IProxyConnection connection = createConnection();
        
        // Act
        connection.doSomething();
        
        // Assert
        assertTrue(connection.isSomething());
    }
}
```

### Testing Connection Events

```java
@Test
@DisplayName("Should notify listener on client connection")
void testClientConnectionEvent() throws InterruptedException {
    MockConnectionListener listener = new MockConnectionListener();
    listener.expectClientConnected();
    
    IProxyConnection connection = createConnection(TEST_MACHINE_ID, listener);
    
    // Trigger client connection...
    
    listener.waitForClientConnected(5000);
    assertTrue(listener.wasClientConnected());
}
```

### Testing Packet Publishing

```java
@Test
@DisplayName("Should publish packets to subscribers")
void testPacketPublishing() throws InterruptedException {
    IProxyConnection connection = createConnection();
    IPacketPublisher publisher = connection.getPacketPublisher();
    
    CountDownLatch latch = new CountDownLatch(1);
    IPacketObserver observer = packet -> {
        assertEquals(0x2001, packet.getOpcode());
        latch.countDown();
    };
    
    publisher.subscribe(0x2001, observer);
    
    // Simulate packet arrival...
    
    waitForLatch(latch, 5000);
}
```

## Test Dependencies

The framework uses the following test dependencies (inherited from parent POM):

- **JUnit 5** (Jupiter) - Test framework
- **Mockito** - Mocking framework

## Best Practices

1. **Always extend ProxyTestBase** - It provides necessary setup and teardown
2. **Use MockConnectionListener** - For testing connection lifecycle
3. **Use packet utilities** - Don't manually construct packet byte arrays
4. **Wait for async operations** - Use latches or wait methods for async tests
5. **Clean up resources** - Base class handles cleanup, but ensure connections are closed
6. **Use descriptive test names** - Use `@DisplayName` for clarity

## Troubleshooting

### Tests Timing Out

- Increase timeout values for `waitFor*` methods
- Check that async operations are actually being triggered
- Verify event loop groups are not blocking

### Mock Issues

- Ensure `@Mock` annotations are processed (use `@ExtendWith(MockitoExtension.class)` if needed)
- Check that mocks are being reset between tests

### Connection Issues

- Verify ports are not in use (use `TEST_PORT` constants)
- Check that event loop groups are properly shut down
- Ensure connections are cleaned up after each test

## Examples

See the following example test files:
- `ProxyConnectionTest.java` - Testing proxy connections
- `ProxyConnectionFactoryTest.java` - Testing connection factory

These examples demonstrate common patterns and best practices for using the testing framework.
