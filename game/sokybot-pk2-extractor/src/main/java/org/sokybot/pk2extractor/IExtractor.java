package org.sokybot.pk2extractor;

import org.sokybot.pk2.IPk2Driver;

/**
 * Interface for pk2 file extractors with streaming support.
 * All extractors output DTOs only - no persistence or caching logic.
 * 
 * <p>Extractors stream DTOs to listeners in real-time as they are parsed,
 * rather than collecting and returning a list. This enables:
 * <ul>
 *   <li>Memory-efficient extraction (no large lists)</li>
 *   <li>Immediate processing of each DTO</li>
 *   <li>Progress tracking for GUI clients</li>
 *   <li>Client-controlled caching</li>
 * </ul>
 * 
 * <p>The extraction runs <b>synchronously</b>. If clients need async behavior,
 * they should run the extraction on a background thread (e.g., SwingWorker,
 * ExecutorService, etc.).
 * 
 * @param <T> The DTO type this extractor produces
 */
public interface IExtractor<T> {
    
    /**
     * Get the DTO class this extractor produces.
     * 
     * @return The Class object for the DTO type
     */
    Class<T> getDtoClass();
    
    /**
     * Get the display name of this extractor.
     * Used for logging and progress reporting.
     * 
     * @return Human-readable name (e.g., "Item Data", "NPC Data")
     */
    String getName();
    
    /**
     * Extract data from pk2 file, streaming results to listeners.
     * 
     * <p>This method runs synchronously and may take significant time
     * for large data sets. Each extracted DTO is immediately passed
     * to the listener's {@code onExtracted} method.
     * 
     * <p>Progress is reported on every extracted item if a progress
     * listener is provided.
     * 
     * @param driver The pk2 driver to read data from
     * @param listener Receives each DTO as it's extracted. May be null
     *                 if caller only wants progress tracking.
     * @param progressListener Receives progress updates for GUI feedback.
     *                         May be null if progress tracking is not needed.
     */
    void extract(IPk2Driver driver, 
                 ExtractionListener<T> listener, 
                 ExtractionProgressListener progressListener);
}
