# Testing Framework for sokybot-game-events

This directory contains the comprehensive testing framework for the `sokybot-game-events` module.

## Structure

```
src/test/java/org/sokybot/gameevents/
├── framework/                           # Test framework utilities
│   ├── PacketTestBuilder.java          # Fluent builder for creating test packets
│   ├── MockGameDataLookup.java         # Mock implementation of IGameDataLookup
│   ├── TranslatorTestBase.java         # Base class for translator tests
│   ├── ChunkedPacketManagerTestHelper.java # Helper for chunked packet testing
│   ├── PacketDataLoader.java           # Interface for loading real packet data (future)
│   └── formats/
│       └── BinaryPacketFormat.java     # Binary packet format loader
├── core/                                # Core component tests
│   ├── ExtensibleTranslatorFactoryTest.java
│   ├── CoreTranslatorProviderTest.java
│   └── ChunkedPacketManagerRegistryTest.java
└── internal/                            # Individual translator tests
    ├── AuthResponseTranslatorTest.java
    ├── CharacterDataBeginTranslatorTest.java
    ├── CharacterDataChunkTranslatorTest.java
    └── EntitySpawnTranslatorTest.java
```

## Framework Components

### PacketTestBuilder

Fluent API for building test packets:

```java
ImmutablePacket packet = PacketTestBuilder.create()
    .putByte(0x01)
    .putInt(12345)
    .putString("test")
    .build();
```

### MockGameDataLookup

Mock implementation for testing translators that need game data:

```java
MockGameDataLookup lookup = new MockGameDataLookup();
lookup.addNPC(12345, "TestMonster");
lookup.addItem(50001, "TestItem");
lookup.addSkill(10001, "TestSkill");
```

### TranslatorTestBase

Base class providing common utilities:

```java
class MyTranslatorTest extends TranslatorTestBase {
    @Test
    void testTranslator() {
        var packet = packetBuilder().putInt(123).build();
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        MyEvent event = assertSingleEvent(events, MyEvent.class);
        // Assert on event fields
    }
}
```

### ChunkedPacketManagerTestHelper

Helper for testing chunked packet translators:

```java
try (ChunkedPacketManagerTestHelper.AutoCloseableHelper helper = 
     new ChunkedPacketManagerTestHelper(TEST_MACHINE_NAME).autoCloseable()) {
    
    // Test chunked packet processing
    helper.assertTransactionActive(BEGIN_OPCODE);
}
```

## Test Coverage

### Core Components (100%)

- ✅ `ExtensibleTranslatorFactory` - Provider registration, priority resolution
- ✅ `CoreTranslatorProvider` - Opcode support, translator creation
- ✅ `ChunkedPacketManagerRegistry` - Registration, retrieval, thread-safety

### Translators (Sample Tests Created)

- ✅ `AuthResponseTranslator` - Session translator
- ✅ `CharacterDataBeginTranslator` - Chunked packet begin
- ✅ `CharacterDataChunkTranslator` - Chunked packet accumulation
- ✅ `EntitySpawnTranslator` - Entity translator with lookup

**Remaining Translators**: 83 more translators need individual tests following the same pattern.

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthResponseTranslatorTest

# Run with coverage
mvn test jacoco:report
```

## Adding New Translator Tests

1. **Create test class** extending `TranslatorTestBase`:

```java
class MyTranslatorTest extends TranslatorTestBase {
    private MyTranslator translator;
    
    @BeforeEach
    void setUp() {
        translator = new MyTranslator(createLookup());
    }
    
    @Test
    @DisplayName("Should parse packet correctly")
    void testParsePacket() {
        var packet = packetBuilder()
            .putInt(12345)
            .putByte(0x01)
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        MyEvent event = assertSingleEvent(events, MyEvent.class);
        // Assert event fields
    }
}
```

2. **Test patterns**:
   - ✅ Successful packet translation
   - ✅ Event creation with correct fields
   - ✅ Error handling (malformed/empty packets)
   - ✅ Edge cases (boundary values, nulls)
   - ✅ Chunked packets (if applicable)

## Hex Dump Packet Testing

The framework now supports loading packets from hex dump files for real packet data testing!

### Hex Dump Format

Hex dump files (`.hex`) contain hexadecimal packet data in text format. Supports multiple formats:

**Plain hex string:**
```
A1 03 01
```

**Hex with comments:**
```
# Auth Response - Success
# Opcode: 0xA103
# Result: 0x01 (success)
01
```

**Hex dump with offsets (auto-detected):**
```
0000: A1 03 01 00 00 00  |......
```

### Usage in Tests

```java
class MyTranslatorTest extends TranslatorTestBase {
    @Test
    void testFromHexDump() throws Exception {
        // Load packet from hex dump file
        var packet = loadHexDump("packets/auth/success_0xA103.hex");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        AuthResponseEvent event = assertSingleEvent(events, AuthResponseEvent.class);
        assertTrue(event.isSuccess());
    }
    
    @Test
    void testFromInlineHex() {
        // Or use inline hex string
        var packet = parseHexString("A1 03 01");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        // ...
    }
}
```

### Test Data Organization

Hex dump files are organized by category:

```
src/test/resources/packets/
├── auth/
│   ├── success_0xA103.hex
│   └── failure_0xA103.hex
├── session/
│   └── login_request_0x6102.hex
├── entity/
│   └── despawn_0x3017.hex
├── stat/
│   ├── level_up_0x3054.hex
│   ├── gold_update_0x304E.hex
│   └── exp_update_0x3056.hex
├── combat/
│   ├── damage_effect_0x3058.hex
│   ├── damage_effect_critical_0x3058.hex
│   └── hp_update_0x3057.hex
└── chat/
    └── all_chat_0x3026.hex
```

### Supported Formats

- ✅ **Hex Dump** (`.hex`) - Hexadecimal text format with comments
- ✅ **Binary** (`.bin`, `.dat`) - Raw packet bytes (via BinaryPacketFormat)
- 🔄 **JSON** - Structured packet data with metadata (planned)

### Hex Dump File Format

Hex dump files support:
- **Comments**: Lines starting with `#` or `//`
- **Multiple formats**: Plain hex, offset-prefixed, ASCII display
- **Whitespace**: Spaces and newlines are ignored
- **Case-insensitive**: Hex digits can be uppercase or lowercase

Example hex dump file:
```
# Auth Response Packet
# Opcode: 0xA103
# Result Code: 0x01 (success)
A1 03 01
```

## Best Practices

1. **Use PacketTestBuilder** for creating test packets (not raw byte arrays)
2. **Extend TranslatorTestBase** for common utilities
3. **Use MockGameDataLookup** when translators need game data
4. **Clean up chunk managers** in `@AfterEach` methods
5. **Test error cases** - malformed packets, empty packets, missing data
6. **Document complex packet structures** in test comments

## Notes

- All packet data uses **little-endian** byte order (Silkroad standard)
- Translators are **thread-safe** - share instances across tests if needed
- Chunk managers are **per-bot** - register/unregister per test machine
- Mock lookups are **thread-safe** - can be shared in concurrent tests
