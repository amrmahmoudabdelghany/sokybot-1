package org.sokybot.pk2extractor.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.test.fixture.ExtractorTestFixture;
import org.sokybot.pk2extractor.test.fixture.ExtractorTestConfiguration;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Base class for compatibility tests of extractors.
 * Tests extractors against real PK2 files from game installations.
 * 
 * <p>Compatibility tests verify that extractors work correctly with:
 * <ul>
 *   <li>Different game versions</li>
 *   <li>Different server configurations</li>
 *   <li>Real PK2 file formats</li>
 * </ul>
 * 
 * <p>These tests are optional and will be skipped if no game directory is configured.
 * 
 * @param <T> The DTO type that the extractor produces
 * 
 * @author sokybot
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractExtractorCompatibilityTest<T> {

    protected static ExtractorTestFixture fixture;
    protected IExtractor<T> extractor;
    protected TestExtractionListener<T> listener;
    protected TestProgressListener progressListener;

    @BeforeAll
    static void setUpFixture() {
        ExtractorTestConfiguration config = ExtractorTestConfiguration.getInstance();
        if (!config.isConfigured()) {
            log.warn("No test configuration found. Compatibility tests will be skipped.");
            return;
        }
        
        try {
            fixture = new ExtractorTestFixture(config);
            log.info("Test fixture initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize test fixture", e);
        }
    }

    @AfterAll
    static void tearDownFixture() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    @BeforeEach
    public void setUp() {
        assumeTrue(fixture != null && fixture.isValid(), 
            "Test fixture not available. Skipping compatibility test.");
        
        extractor = createExtractor();
        listener = new TestExtractionListener<>();
        progressListener = new TestProgressListener();
    }

    /**
     * Create the extractor instance to test.
     * Subclasses must implement this method.
     * 
     * @return The extractor instance
     */
    protected abstract IExtractor<T> createExtractor();

    /**
     * Get the PK2 driver to use for testing.
     * Default implementation returns Media.pk2 driver.
     * Subclasses can override to use different PK2 files.
     * 
     * @return The PK2 driver
     */
    protected IPk2Driver getPk2Driver() {
        return fixture.getMediaPk2();
    }

    /**
     * Get the expected minimum number of items for this extractor.
     * Used for basic validation. Default is 1.
     * 
     * @return Minimum expected item count
     */
    protected int getMinimumExpectedItemCount() {
        return 1;
    }

    @Test
    @Order(1)
    @DisplayName("Extractor can extract from Media.pk2")
    public void testExtractFromMediaPk2() {
        IPk2Driver driver = fixture.getMediaPk2();
        assertNotNull(driver, "Media.pk2 driver should be available");

        extractor.extract(driver, listener, progressListener);

        assertFalse(listener.hasError(), 
            "Extraction should not fail. Error: " + 
            (listener.getError() != null ? listener.getError().getMessage() : "unknown"));
        
        assertTrue(progressListener.isStarted(), "Progress listener should receive start event");
        assertTrue(progressListener.isCompleted(), "Progress listener should receive complete event");
    }

    @Test
    @Order(2)
    @DisplayName("Extractor extracts valid items")
    public void testExtractsValidItems() {
        IPk2Driver driver = getPk2Driver();
        
        extractor.extract(driver, listener, progressListener);

        int itemCount = listener.getExtractedItems().size();
        assertTrue(itemCount >= getMinimumExpectedItemCount(),
            String.format("Should extract at least %d items, but extracted %d",
                getMinimumExpectedItemCount(), itemCount));
        
        assertEquals(listener.getCompleteCount(), itemCount,
            "Complete count should match extracted items count");
    }

    @Test
    @Order(3)
    @DisplayName("Extracted items have valid data")
    public void testExtractedItemsHaveValidData() {
        IPk2Driver driver = getPk2Driver();
        
        extractor.extract(driver, listener, progressListener);

        List<T> items = listener.getExtractedItems();
        assertFalse(items.isEmpty(), "Should have extracted at least one item");

        // Validate first few items
        int itemsToValidate = Math.min(10, items.size());
        for (int i = 0; i < itemsToValidate; i++) {
            T item = items.get(i);
            assertNotNull(item, String.format("Item %d should not be null", i));
            validateItem(item, i);
        }
    }

    /**
     * Validate a single extracted item.
     * Subclasses should override to perform specific validation.
     * 
     * @param item The item to validate
     * @param index The index of the item in the extraction list
     */
    protected void validateItem(T item, int index) {
        // Default: just check not null
        assertNotNull(item, "Item should not be null");
    }

    @Test
    @Order(4)
    @DisplayName("Progress listener receives correct events")
    public void testProgressListenerReceivesEvents() {
        IPk2Driver driver = getPk2Driver();
        
        extractor.extract(driver, listener, progressListener);

        assertTrue(progressListener.isStarted(), "Should receive start event");
        assertTrue(progressListener.isCompleted(), "Should receive complete event");
        
        assertFalse(progressListener.getEvents().isEmpty(), 
            "Should have at least one progress event");
    }

    @Test
    @Order(5)
    @DisplayName("Extractor works with null listeners")
    public void testWorksWithNullListeners() {
        IPk2Driver driver = getPk2Driver();
        
        // Should not throw exceptions when listeners are null
        assertDoesNotThrow(() -> {
            extractor.extract(driver, null, null);
        }, "Should handle null listeners gracefully");
    }

    /**
     * Reuse the test listener classes from unit test base.
     */
    protected static class TestExtractionListener<T> implements ExtractionListener<T> {
        private final List<T> extractedItems = new java.util.ArrayList<>();
        private final java.util.concurrent.atomic.AtomicInteger completeCount = new java.util.concurrent.atomic.AtomicInteger(0);
        private Throwable error;

        @Override
        public void onExtracted(T item) {
            extractedItems.add(item);
        }

        @Override
        public void onComplete(int totalCount) {
            completeCount.set(totalCount);
        }

        @Override
        public void onError(Exception e) {
            this.error = e;
        }

        public List<T> getExtractedItems() {
            return extractedItems;
        }

        public int getCompleteCount() {
            return completeCount.get();
        }

        public Throwable getError() {
            return error;
        }

        public boolean hasError() {
            return error != null;
        }
    }

    protected static class TestProgressListener implements ExtractionProgressListener {
        private final List<ProgressEvent> events = new java.util.ArrayList<>();
        private String extractorName;
        private boolean started = false;
        private boolean completed = false;

        @Override
        public void onStart(String extractorName, int totalItems) {
            this.extractorName = extractorName;
            this.started = true;
            events.add(new ProgressEvent(ProgressEventType.START, extractorName, 0, totalItems, null));
        }

        @Override
        public void onProgress(String extractorName, int current, int total, String currentItemId) {
            events.add(new ProgressEvent(ProgressEventType.PROGRESS, extractorName, current, total, currentItemId));
        }

        @Override
        public void onComplete(String extractorName, int totalItems, long durationMs) {
            this.completed = true;
            events.add(new ProgressEvent(ProgressEventType.COMPLETE, extractorName, totalItems, totalItems, durationMs));
        }

        @Override
        public void onError(String extractorName, Exception e) {
            events.add(new ProgressEvent(ProgressEventType.ERROR, extractorName, -1, -1, e));
        }

        public List<ProgressEvent> getEvents() {
            return events;
        }

        public String getExtractorName() {
            return extractorName;
        }

        public boolean isStarted() {
            return started;
        }

        public boolean isCompleted() {
            return completed;
        }

        public enum ProgressEventType {
            START, PROGRESS, COMPLETE, ERROR
        }

        public static class ProgressEvent {
            private final ProgressEventType type;
            private final String extractorName;
            private final int current;
            private final int total;
            private final Object data;

            public ProgressEvent(ProgressEventType type, String extractorName, int current, int total, Object data) {
                this.type = type;
                this.extractorName = extractorName;
                this.current = current;
                this.total = total;
                this.data = data;
            }

            public ProgressEventType getType() {
                return type;
            }

            public String getExtractorName() {
                return extractorName;
            }

            public int getCurrent() {
                return current;
            }

            public int getTotal() {
                return total;
            }

            public Object getData() {
                return data;
            }
        }
    }
}
