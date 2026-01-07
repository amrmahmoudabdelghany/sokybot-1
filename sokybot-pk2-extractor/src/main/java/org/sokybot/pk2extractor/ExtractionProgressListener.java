package org.sokybot.pk2extractor;

/**
 * Listener interface for tracking extraction progress.
 * Useful for GUI clients that want to display progress bars, status labels,
 * or other visual indicators during long-running extraction processes.
 * 
 * <p>Progress is reported on every single extracted item for maximum responsiveness.
 * 
 * <p>Example usage with Swing:
 * <pre>{@code
 * extractor.extract(driver, dtoListener, new ExtractionProgressListener() {
 *     @Override
 *     public void onStart(String extractorName, int estimatedTotal) {
 *         SwingUtilities.invokeLater(() -> {
 *             progressBar.setMaximum(estimatedTotal > 0 ? estimatedTotal : 100);
 *             statusLabel.setText("Starting " + extractorName + "...");
 *         });
 *     }
 *     
 *     @Override
 *     public void onProgress(String extractorName, int current, int total, String itemId) {
 *         SwingUtilities.invokeLater(() -> {
 *             progressBar.setValue(current);
 *             statusLabel.setText(extractorName + ": " + current + "/" + total);
 *         });
 *     }
 *     
 *     @Override
 *     public void onComplete(String extractorName, int totalExtracted, long durationMs) {
 *         SwingUtilities.invokeLater(() -> {
 *             progressBar.setValue(progressBar.getMaximum());
 *             statusLabel.setText("Complete: " + totalExtracted + " items in " + durationMs + "ms");
 *         });
 *     }
 *     
 *     @Override
 *     public void onError(String extractorName, Exception error) {
 *         SwingUtilities.invokeLater(() -> {
 *             statusLabel.setText("Error: " + error.getMessage());
 *         });
 *     }
 * });
 * }</pre>
 */
public interface ExtractionProgressListener {
    
    /**
     * Called when an extraction phase starts.
     * 
     * @param extractorName Display name of the extractor (e.g., "Item Data", "NPC Data")
     * @param estimatedTotal Estimated total items to extract. 
     *                       May be -1 if unknown at start time.
     */
    void onStart(String extractorName, int estimatedTotal);
    
    /**
     * Called for every single extracted item.
     * This provides maximum responsiveness for GUI updates.
     * 
     * @param extractorName Display name of the extractor
     * @param current Current item number (1-based)
     * @param total Total items. May update as extraction progresses.
     *              Will be -1 if total is still unknown.
     * @param currentItemId Optional identifier of the current item being processed
     *                      (e.g., longId or refId). May be null.
     */
    void onProgress(String extractorName, int current, int total, String currentItemId);
    
    /**
     * Called when extraction phase completes successfully.
     * 
     * @param extractorName Display name of the extractor
     * @param totalExtracted Total items successfully extracted
     * @param durationMs Time taken in milliseconds
     */
    void onComplete(String extractorName, int totalExtracted, long durationMs);
    
    /**
     * Called if extraction fails with an error.
     * 
     * @param extractorName Display name of the extractor
     * @param error The exception that caused the failure
     */
    void onError(String extractorName, Exception error);
}
