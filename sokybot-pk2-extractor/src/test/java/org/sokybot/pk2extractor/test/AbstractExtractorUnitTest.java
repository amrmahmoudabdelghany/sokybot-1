package org.sokybot.pk2extractor.test;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for unit tests of extractors.
 * Provides common setup and utilities for testing extractor implementations.
 * 
 * <p>This class provides:
 * <ul>
 *   <li>Mocked PK2 driver setup</li>
 *   <li>Test listeners for capturing extraction results</li>
 *   <li>Progress tracking utilities</li>
 *   <li>Common assertions</li>
 * </ul>
 * 
 * @param <T> The DTO type that the extractor produces
 * 
 * @author sokybot
 */
public abstract class AbstractExtractorUnitTest<T> {

    @Mock
    protected IPk2Driver mockDriver;

    protected IExtractor<T> extractor;
    protected TestExtractionListener<T> testListener;
    protected TestProgressListener testProgressListener;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        extractor = createExtractor();
        testListener = new TestExtractionListener<>();
        testProgressListener = new TestProgressListener();
    }

    /**
     * Create the extractor instance to test.
     * Subclasses must implement this method.
     * 
     * @return The extractor instance
     */
    protected abstract IExtractor<T> createExtractor();

    /**
     * Test listener that captures all extracted DTOs.
     */
    public static class TestExtractionListener<T> implements ExtractionListener<T> {
        private final List<T> extractedItems = new ArrayList<>();
        private final AtomicInteger completeCount = new AtomicInteger(0);
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

        public void clear() {
            extractedItems.clear();
            completeCount.set(0);
            error = null;
        }
    }

    /**
     * Test progress listener that tracks extraction progress.
     */
    public static class TestProgressListener implements ExtractionProgressListener {
        private final List<ProgressEvent> events = new ArrayList<>();
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

        public void clear() {
            events.clear();
            started = false;
            completed = false;
            extractorName = null;
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
