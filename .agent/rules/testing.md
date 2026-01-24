---
trigger: always_on
glob: "**/src/test/**/*.java"
description: Testing guidelines and patterns
---

# Testing Guidelines

## 1. Test Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Unit Test | `*Test.java` | `PacketHandlerTest.java` |
| Integration Test | `*IT.java` | `ProxyIntegrationIT.java` |
| Integration Test | `*IntegrationTest.java` | `DatabaseIntegrationTest.java` |

## 2. Test Structure (Given-When-Then)

```java
@Test
void shouldProcessPacketWhenValidData() {
    // Given (Arrange)
    byte[] packetData = createValidPacket();
    PacketHandler handler = new PacketHandler();
    
    // When (Act)
    Result result = handler.process(packetData);
    
    // Then (Assert)
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getData()).hasSize(10);
}
```

## 3. Test Method Naming

Use descriptive names:

```java
// Good
void shouldReturnEmptyListWhenNoPlayersOnline()
void shouldThrowExceptionWhenPacketIsMalformed()
void shouldDecryptPacketWithBlowfishKey()

// Bad
void test1()
void testProcess()
void testHandlePacket()
```

## 4. JUnit 5 Features

### Annotations
```java
@Test                    // Test method
@DisplayName("...")      // Readable name
@BeforeEach             // Setup before each test
@AfterEach              // Cleanup after each test
@BeforeAll              // Setup once for class
@AfterAll               // Cleanup once for class
@Disabled("reason")     // Skip test
@Tag("integration")     // Categorize tests
```

### Parameterized Tests
```java
@ParameterizedTest
@ValueSource(ints = {1, 2, 3, 4, 5})
void shouldHandleMultipleValues(int value) {
    assertThat(processor.process(value)).isNotNull();
}

@ParameterizedTest
@CsvSource({
    "input1, expected1",
    "input2, expected2"
})
void shouldMapInputToOutput(String input, String expected) {
    assertThat(mapper.map(input)).isEqualTo(expected);
}
```

## 5. Mockito Usage

### Basic Mocking
```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    
    @Mock
    private IDependency dependency;
    
    @InjectMocks
    private MyService service;
    
    @Test
    void shouldCallDependency() {
        // Given
        when(dependency.getData()).thenReturn("test");
        
        // When
        String result = service.process();
        
        // Then
        verify(dependency).getData();
        assertThat(result).contains("test");
    }
}
```

### Argument Matchers
```java
when(service.find(anyString())).thenReturn(result);
when(service.find(eq("specific"))).thenReturn(specificResult);
when(service.find(argThat(s -> s.startsWith("prefix")))).thenReturn(prefixResult);
```

### Verification
```java
verify(mock).method();                    // Called once
verify(mock, times(2)).method();          // Called twice
verify(mock, never()).method();           // Never called
verify(mock, atLeastOnce()).method();     // Called at least once
verifyNoMoreInteractions(mock);           // No other calls
```

## 6. Assertions (AssertJ Preferred)

```java
// AssertJ (preferred)
assertThat(result).isNotNull();
assertThat(result.getName()).isEqualTo("expected");
assertThat(list).hasSize(3).contains("a", "b");
assertThat(map).containsKey("key").containsValue("value");
assertThatThrownBy(() -> service.process(null))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("null");

// JUnit 5 (acceptable)
assertEquals(expected, actual);
assertTrue(condition);
assertThrows(Exception.class, () -> riskyOperation());
```

## 7. Test Categories

### Unit Tests
- Test single class in isolation
- Mock all dependencies
- Fast execution (<100ms)
- No external resources

### Integration Tests
- Test multiple components together
- May use real dependencies
- May be slower
- Use `@Tag("integration")`

### OSGi Tests
- Test bundle activation/services
- Use Pax Exam or similar
- Slower, requires OSGi runtime

## 8. Test Data

### Test Fixtures
```java
class TestFixtures {
    static Packet createValidPacket() {
        return Packet.builder()
            .id(0x1234)
            .data(new byte[]{0x01, 0x02, 0x03})
            .build();
    }
}
```

### Resource Files
Place in `src/test/resources/`:
```
src/test/resources/
├── test-data/
│   ├── valid-packet.bin
│   └── malformed-packet.bin
└── test-config.properties
```

Load with:
```java
InputStream is = getClass().getResourceAsStream("/test-data/valid-packet.bin");
```

## 9. Test Coverage

Target coverage levels:
- **API/Interfaces**: 80%+
- **Core logic**: 70%+
- **Utilities**: 60%+
- **Generated code**: Not required

Run coverage:
```bash
./mvnw verify -P ci
# Report at target/site/jacoco/index.html
```

## 10. Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Testing implementation details | Test behavior, not internals |
| Excessive mocking | Test with real objects when practical |
| Brittle tests | Avoid over-specification |
| Slow tests | Mock I/O, use in-memory databases |
| Flaky tests | Avoid timing dependencies, use proper synchronization |
| No assertions | Every test must assert something |
