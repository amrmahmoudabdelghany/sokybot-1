package org.sokybot.pk2extractor.mediapk2.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.region.RegionData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for RegionDataExtractor.
 */
@DisplayName("RegionDataExtractor Unit Tests")
class RegionDataExtractorUnitTest extends AbstractExtractorUnitTest<RegionData> {

    @Override
    protected IExtractor<RegionData> createExtractor() {
        return new RegionDataExtractor();
    }

    @Test
    @DisplayName("Should extract region data from CSV")
    void testExtractRegionData() {
        // Format: service, id, longId, name, type, areaId, pvp, safe, minLvl, maxLvl
        String content = "1\t100\tREGION_TEST\tTest Region\t1\t50\t0\t1\t1\t100";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("regioninfo.txt", content)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one region");

        RegionData region = testListener.getExtractedItems().get(0);
        assertNotNull(region);
        assertEquals(100, region.getId());
        assertEquals("REGION_TEST", region.getLongId());
        assertEquals("Test Region", region.getName());
    }

    @Test
    @DisplayName("Should skip comments and short lines")
    void testSkipInvalidLines() {
        StringBuilder sb = new StringBuilder();
        sb.append("// This is a comment\n");
        sb.append("1\t2\t3\n"); // Too short
        sb.append("1\t100\tREGION_VALID\tValid Region\t1\t50\t0\t1");

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("regioninfo.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError());
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one valid region");
    }

    @Test
    @DisplayName("Should handle missing file gracefully")
    void testMissingFile() {
        IPk2Driver driver = new MockPk2DriverBuilder().build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Should not fail on missing file");
        assertEquals(0, testListener.getExtractedItems().size());
    }
}
