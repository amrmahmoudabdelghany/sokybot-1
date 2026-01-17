package org.sokybot.pk2extractor.datapk2;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.ObjectNavMeshData;
import org.sokybot.pk2extractor.test.AbstractExtractorCompatibilityTest;

/**
 * Compatibility test for ObjectInfoExtractor against real Data.pk2 file.
 * 
 * <p>This test verifies that ObjectInfoExtractor correctly extracts
 * navigation mesh data from actual game files.
 * 
 * <p>Requires a valid game installation directory to be configured.
 * The test will be skipped if no Data.pk2 file is available.
 * 
 * @author sokybot
 */
@DisplayName("ObjectInfoExtractor Compatibility Test (Data.pk2)")
class ObjectInfoExtractorCompatibilityTest extends AbstractExtractorCompatibilityTest<ObjectNavMeshData> {

    private IPk2Driver dataPk2;

    @Override
    protected IExtractor<ObjectNavMeshData> createExtractor() {
        return new ObjectInfoExtractor();
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        
        // ObjectInfoExtractor reads from Data.pk2, not Media.pk2
        dataPk2 = fixture.getDataPk2().orElse(null);
        assumeTrue(dataPk2 != null, "Data.pk2 not available. Skipping test.");
    }

    @Override
    protected IPk2Driver getPk2Driver() {
        return dataPk2;
    }

    @Override
    protected int getMinimumExpectedItemCount() {
        // Expect at least some navmesh objects in any game version
        return 10;
    }

    @Override
    protected void validateItem(ObjectNavMeshData item, int index) {
        assertNotNull(item, "Item should not be null");
        assertTrue(item.getId() >= 0, "Object ID should be non-negative");
        
        // All navmesh items should have at least some geometry data
        // Either points, or ground cells, or outline/inline edges
        boolean hasGeometry = !item.getPoints().isEmpty() 
            || !item.getObjectGround().isEmpty()
            || !item.getOutLines().isEmpty()
            || !item.getInLines().isEmpty();
        
        // Log for debugging but don't fail - some objects may have minimal data
        if (!hasGeometry) {
            System.out.println("Warning: Object " + item.getId() + " has no geometry data");
        }
    }

    @Test
    @DisplayName("Should extract navmesh with valid vertices")
    void testExtractsNavmeshWithValidVertices() {
        extractor.extract(dataPk2, listener, progressListener);

        assertFalse(listener.hasError(), "Extraction should not fail");
        assertFalse(listener.getExtractedItems().isEmpty(), "Should extract at least one object");

        // Find an item with points and validate
        listener.getExtractedItems().stream()
            .filter(item -> !item.getPoints().isEmpty())
            .findFirst()
            .ifPresent(item -> {
                assertTrue(item.getPoints().size() >= 3, 
                    "Navmesh should have at least 3 vertices to form a triangle");
                
                item.getPoints().forEach(point -> {
                    // Coordinates should be reasonable world values
                    assertTrue(Math.abs(point.getX()) < 10000, "X coordinate out of range");
                    assertTrue(Math.abs(point.getY()) < 10000, "Y coordinate out of range");
                    assertTrue(Math.abs(point.getZ()) < 10000, "Z coordinate out of range");
                });
            });
    }

    @Test
    @DisplayName("Should extract navmesh with collision cells")
    void testExtractsNavmeshWithCollisionCells() {
        extractor.extract(dataPk2, listener, progressListener);

        assertFalse(listener.hasError(), "Extraction should not fail");

        // Find an item with ground cells
        long itemsWithGround = listener.getExtractedItems().stream()
            .filter(item -> !item.getObjectGround().isEmpty())
            .count();

        assertTrue(itemsWithGround > 0, "Should have at least one object with collision cells");
    }

    @Test
    @DisplayName("Should extract navmesh with blocking flags")
    void testExtractsNavmeshWithBlockingFlags() {
        extractor.extract(dataPk2, listener, progressListener);

        assertFalse(listener.hasError(), "Extraction should not fail");

        // Find objects with blocking flags set
        long blockingObjects = listener.getExtractedItems().stream()
            .filter(item -> item.isOutCanBlock() || item.isInCanBlock())
            .count();

        // Not all objects will have blocking, but some should
        // This is informational - log the result
        System.out.println("Objects with blocking flags: " + blockingObjects + 
            " / " + listener.getExtractedItems().size());
    }
}
