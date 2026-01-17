# Sokybot Runtime Test Framework

This directory contains the test framework for the `sokybot-runtime` bundle. The framework provides utilities and base classes for testing OSGi-based runtime components.

## Structure

```
src/test/java/org/sokybot/runtime/
├── test/
│   ├── RuntimeTestBase.java        # Base class for all runtime tests
│   └── util/
│       └── OSGiTestUtils.java      # OSGi mocking utilities
└── internal/
    ├── SokybotContextImplTest.java      # Tests for SokybotContextImpl
    ├── GroupContextImplTest.java        # Tests for GroupContextImpl
    └── GroupContextFactoryImplTest.java # Tests for GroupContextFactoryImpl
```

## Test Framework Components

### RuntimeTestBase

Base class for all runtime tests that provides:
- Mock OSGi services (EventAdmin, IRuteFinderFactory, IGamePersistenceFactory, etc.)
- Test data creation helpers (`createTestGroupInfo()`, `createTestMachineInfo()`)
- Standard test constants (TEST_GROUP_NAME, TEST_MACHINE_NAME, etc.)
- Setup/teardown infrastructure

**Usage:**
```java
@ExtendWith(MockitoExtension.class)
class MyTest extends RuntimeTestBase {
    
    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        // Your custom setup
    }
    
    @Test
    void myTest() {
        GroupInfo groupInfo = createTestGroupInfo();
        // Test code
    }
}
```

### OSGiTestUtils

Provides mock implementations of OSGi framework classes:
- `MockBundleContext` - Mock BundleContext with service registry
- `createMockBundleContext()` - Factory method for creating mock contexts

**Features:**
- Service registration and lookup
- Service reference management
- Bundle state management

**Usage:**
```java
MockBundleContext mockContext = OSGiTestUtils.createMockBundleContext();
mockContext.registerMockService(EventAdmin.class, mockEventAdmin, null);
EventAdmin service = mockContext.getService(mockContext.getServiceReference(EventAdmin.class));
```

## Writing Tests

### Example: Testing GroupContextImpl

```java
class GroupContextImplTest extends RuntimeTestBase {
    
    private GroupContextImpl groupContext;
    private GroupInfo groupInfo;
    
    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        
        groupInfo = createTestGroupInfo();
        groupContext = new GroupContextImpl(
            groupInfo,
            mockBundleContext,
            mockEventAdmin,
            mockRuteFinderFactory,
            mockGamePersistenceFactory
        );
    }
    
    @Test
    void testGetGameDataLookup() {
        IGameDataLookup lookup = groupContext.getGameDataLookup();
        
        assertNotNull(lookup);
        verify(mockGamePersistenceFactory).getLookup(TEST_GAME_PATH);
    }
}
```

### Example: Testing Event Publishing

```java
@Test
void testInstallMachinePublishesEvent() {
    ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
    
    groupContext.installMachine("test-machine");
    
    verify(mockEventAdmin).postEvent(eventCaptor.capture());
    Event event = eventCaptor.getValue();
    assertEquals(ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED, event.getTopic());
}
```

## Test Categories

### Unit Tests
- Test individual components in isolation
- Use mocks for all dependencies
- Fast execution

### Integration Tests
- Test component interactions
- May use real implementations where appropriate
- Test OSGi service lifecycle

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SokybotContextImplTest

# Run tests with coverage
mvn test jacoco:report
```

## Best Practices

1. **Extend RuntimeTestBase** - Provides common setup and utilities
2. **Use Mockito for mocking** - Follows project standards
3. **Test both success and failure paths** - Include negative test cases
4. **Verify interactions** - Use `verify()` to check service calls
5. **Clean up resources** - Implement proper teardown in `@AfterEach`
6. **Use descriptive test names** - Follow `test<Method>_<Scenario>` pattern

## Mock Services

The test framework provides mocks for:
- `EventAdmin` - For event publishing tests
- `IRuteFinderFactory` - For navigation tests
- `IGamePersistenceFactory` - For persistence tests
- `IGameDataLookup` - For game data lookup tests
- `BundleContext` - For OSGi service access tests

## Notes

- Tests run in isolated JVM - no real OSGi framework required
- All OSGi services are mocked using `MockBundleContext`
- Test data uses `@TempDir` for temporary file operations
- Tests follow JUnit 5 conventions with Mockito extensions
