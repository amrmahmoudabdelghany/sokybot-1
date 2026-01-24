package org.sokybot.pk2extractor.mediapk2.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.teleport.TeleportData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for TeleportDataExtractor.
 */
@DisplayName("TeleportDataExtractor Unit Tests")
class TeleportDataExtractorUnitTest extends AbstractExtractorUnitTest<TeleportData> {

    @Override
    protected IExtractor<TeleportData> createExtractor() {
        return new TeleportDataExtractor();
    }

    @Test
    @DisplayName("Should extract teleport data from CSV")
    void testExtractTeleportData() {
        // teleportdata.txt format: service, refId, longId, portalId, name
        String dataContent = "1\t100\tTELEPORT_TEST\t50\tTest Teleport";
        // teleportlink.txt format: service, id, linkedId
        String linkContent = "1\t100\t200";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("teleportdata.txt", dataContent)
                .withFile("teleportlink.txt", linkContent)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one teleport");

        TeleportData teleport = testListener.getExtractedItems().get(0);
        assertNotNull(teleport);
        assertEquals(100, teleport.getRefId());
        assertEquals("TELEPORT_TEST", teleport.getLongId());
        assertEquals(50, teleport.getPortalId());
        assertEquals("Test Teleport", teleport.getName());
        assertTrue(teleport.getLinks().contains(200), "Should contain link to 200");
    }

    @Test
    @DisplayName("Should handle missing link file gracefully")
    void testMissingLinkFile() {
        String dataContent = "1\t100\tTELEPORT_TEST\t50\tTest Teleport";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("teleportdata.txt", dataContent)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Should not fail on missing link file");
        assertEquals(1, testListener.getExtractedItems().size());
        
        TeleportData teleport = testListener.getExtractedItems().get(0);
        assertTrue(teleport.getLinks().isEmpty(), "Should have no links");
    }

    @Test
    @DisplayName("Should handle missing data file gracefully")
    void testMissingDataFile() {
        IPk2Driver driver = new MockPk2DriverBuilder().build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Should not fail on missing data file");
        assertEquals(0, testListener.getExtractedItems().size());
    }
}
