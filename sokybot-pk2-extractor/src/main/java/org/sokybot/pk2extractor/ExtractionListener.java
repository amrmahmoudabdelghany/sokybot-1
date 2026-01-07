package org.sokybot.pk2extractor;

/**
 * Listener interface for receiving extracted DTOs in real-time as they are parsed.
 * Clients implement this to receive each DTO immediately as it's extracted,
 * rather than waiting for the entire extraction to complete.
 * 
 * <p>This enables:
 * <ul>
 *   <li>Immediate processing without waiting for completion</li>
 *   <li>Memory-efficient streaming (no need to hold all DTOs in memory)</li>
 *   <li>Client-managed caching (if needed)</li>
 *   <li>Real-time GUI updates</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * extractor.extract(driver, new ExtractionListener<ItemData>() {
 *     @Override
 *     public void onExtracted(ItemData dto) {
 *         // Process immediately - save to DB, cache, display, etc.
 *         entityMapper.mapAndSave(dto);
 *     }
 *     
 *     @Override
 *     public void onComplete(int totalCount) {
 *         log.info("Extraction complete: {} items", totalCount);
 *     }
 *     
 *     @Override
 *     public void onError(Exception error) {
 *         log.error("Extraction failed", error);
 *     }
 * }, null);
 * }</pre>
 * 
 * @param <T> The DTO type this listener handles
 */
public interface ExtractionListener<T> {
    
    /**
     * Called immediately when a single DTO is successfully extracted.
     * This method is called once for each extracted item, allowing
     * real-time processing as extraction progresses.
     * 
     * @param dto The extracted data transfer object
     */
    void onExtracted(T dto);
    
    /**
     * Called when extraction for this type is complete.
     * This is always called after all {@code onExtracted} calls,
     * assuming no error occurred.
     * 
     * @param totalCount Total number of DTOs successfully extracted
     */
    void onComplete(int totalCount);
    
    /**
     * Called if extraction fails with an error.
     * After this is called, no more {@code onExtracted} calls will occur.
     * Note that some DTOs may have been successfully extracted before the error.
     * 
     * @param error The exception that caused extraction to fail
     */
    void onError(Exception error);
}
