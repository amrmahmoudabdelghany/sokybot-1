package org.sokybot.pk2extractor.mediapk2.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.shop.ShopGroupData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for ShopGroupExtractor.
 */
@DisplayName("ShopGroupExtractor Unit Tests")
class ShopGroupExtractorUnitTest extends AbstractExtractorUnitTest<ShopGroupData> {

    @Override
    protected IExtractor<ShopGroupData> createExtractor() {
        return new ShopGroupExtractor();
    }

    @Test
    @DisplayName("Should extract shop group data")
    void testExtractShopGroupData() {
        // Prepare mock data
        // Format: // comments...
        // 1\t100\tGROUP_CODE\tNPC_REF
        StringBuilder sb = new StringBuilder();
        sb.append("1\t1\t100\tGROUP_TEST\tNPC_TEST_REF\n");
        // Add another line with different data
        sb.append("1\t1\t101\tGROUP_TEST_2\tNPC_TEST_REF_2");

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("refshopgroup.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(2, testListener.getExtractedItems().size(), "Should extract two items");

        ShopGroupData group1 = testListener.getExtractedItems().get(0);
        assertNotNull(group1);
        assertEquals(1, group1.getCountry());
        assertEquals(100, group1.getId());
        assertEquals("GROUP_TEST", group1.getCodeName());
        assertEquals("NPC_TEST_REF", group1.getRefNpcCodeName());
    }

    @Test
    @DisplayName("Should skip comments and invalid lines")
    void testSkipInvalidLines() {
        StringBuilder sb = new StringBuilder();
        sb.append("// This is a comment\n");
        sb.append("Invalid Line Here\n");
        sb.append("1\t1\t100\tGROUP_TEST\tNPC_TEST_REF");

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("refshopgroup.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one valid item");
    }

    @Test
    @DisplayName("Should handle missing file gracefully")
    void testMissingFile() {
        // No match for pattern (?i)refshopgroup.*\.txt$
        IPk2Driver driver = new MockPk2DriverBuilder().build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Should not fail on missing file");
        assertEquals(0, testListener.getExtractedItems().size(), "Should not extract items");
    }
}
