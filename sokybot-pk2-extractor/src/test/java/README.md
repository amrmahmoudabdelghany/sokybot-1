# PK2 Extractor Test Framework

This directory contains the test framework for the `sokybot-pk2-extractor` module. The framework provides support for both **unit tests** and **compatibility tests**.

## Structure

```
src/test/java/org/sokybot/pk2extractor/
├── test/
│   ├── AbstractExtractorUnitTest.java      # Base class for unit tests
│   ├── AbstractExtractorCompatibilityTest.java  # Base class for compatibility tests
│   ├── fixture/
│   │   ├── ExtractorTestFixture.java       # Test fixture for PK2 access
│   │   └── ExtractorTestConfiguration.java # Test configuration loader
│   └── util/
│       └── MockPk2DriverBuilder.java       # Builder for mock PK2 drivers
└── [package]/
    └── [Extractor]Test.java                # Example test implementations
```

## Unit Tests

Unit tests test extractors in isolation using mocked PK2 drivers. They are fast and don't require actual game files.

### Creating a Unit Test

1. Extend `AbstractExtractorUnitTest<T>` where `T` is your DTO type:

```java
class MyExtractorUnitTest extends AbstractExtractorUnitTest<MyDto> {
    
    @Override
    protected IExtractor<MyDto> createExtractor() {
        return new MyExtractor();
    }
    
    @Test
    void testExtract() {
        String csvContent = "...";
        IPk2Driver driver = new MockPk2DriverBuilder()
            .withFile("mydata.txt", csvContent)
            .build();
        
        extractor.extract(driver, testListener, testProgressListener);
        
        assertEquals(1, testListener.getExtractedItems().size());
    }
}
```

2. Use `MockPk2DriverBuilder` to create mock PK2 drivers with test data
3. Use `testListener` and `testProgressListener` provided by the base class

## Compatibility Tests

Compatibility tests test extractors against real PK2 files from game installations. They verify that extractors work correctly with actual game data.

### Configuration

Compatibility tests require a game directory to be configured. You can provide it in one of the following ways:

1. **System Property**: `-Dsokybot.test.game.directory=/path/to/game`
2. **Environment Variable**: `SOKYBOT_TEST_GAME_DIRECTORY=/path/to/game`
3. **Properties File**: Create `extractor-test.properties` in your home directory:
   ```properties
   game.directory=/path/to/game
   ```

If no configuration is found, compatibility tests will be skipped.

### Creating a Compatibility Test

1. Extend `AbstractExtractorCompatibilityTest<T>`:

```java
class MyExtractorCompatibilityTest extends AbstractExtractorCompatibilityTest<MyDto> {
    
    @Override
    protected IExtractor<MyDto> createExtractor() {
        return new MyExtractor();
    }
    
    @Override
    protected int getMinimumExpectedItemCount() {
        return 100; // Minimum expected items
    }
    
    @Override
    protected void validateItem(MyDto item, int index) {
        super.validateItem(item, index);
        // Add custom validation logic
        assertNotNull(item.getSomeField());
    }
}
```

2. The base class provides several test methods that verify:
   - Extractor can extract from Media.pk2
   - Extracted items are valid
   - Progress listeners work correctly
   - Null listeners are handled gracefully

## Running Tests

### Run Unit Tests Only

```bash
mvn test
```

### Run Compatibility Tests

First, configure the game directory (see Configuration section above), then:

```bash
mvn test
```

Compatibility tests will be skipped automatically if no configuration is available.

### Run Specific Test Class

```bash
mvn test -Dtest=ItemDataExtractorUnitTest
```

## Example Tests

See the following example implementations:

- `ItemDataExtractorUnitTest.java` - Example unit test
- `ItemDataExtractorCompatibilityTest.java` - Example compatibility test

## Best Practices

1. **Unit Tests**: Fast, isolated tests that use mocks. Test edge cases, error handling, and parsing logic.

2. **Compatibility Tests**: Integration tests with real data. Test that extractors work with actual game files. Override `validateItem()` to add custom validation.

3. **Test Data**: Use `MockPk2DriverBuilder` for unit tests. Keep test data minimal and focused on specific scenarios.

4. **Assertions**: Use meaningful assertion messages that describe what failed and why.

5. **Error Handling**: Test that extractors handle errors gracefully (empty files, malformed data, missing files, etc.).

## Framework Features

### AbstractExtractorUnitTest

- Provides mocked PK2 driver (`mockDriver`)
- Provides test listeners (`testListener`, `testProgressListener`)
- Common setup and teardown

### AbstractExtractorCompatibilityTest

- Provides real PK2 driver access via fixture
- Automatic configuration loading
- Test ordering and grouping
- Progress tracking verification

### MockPk2DriverBuilder

- Easy creation of mock PK2 drivers
- Support for multiple files
- Pattern matching for file finding
- Custom charset support

### ExtractorTestFixture

- Wraps PK2 test fixture
- Provides access to Media.pk2 and Data.pk2
- File finding utilities
- Automatic cleanup
