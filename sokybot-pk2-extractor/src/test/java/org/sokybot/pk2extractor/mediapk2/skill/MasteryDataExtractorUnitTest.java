package org.sokybot.pk2extractor.mediapk2.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.skill.MasteryData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for MasteryDataExtractor.
 */
@DisplayName("MasteryDataExtractor Unit Tests")
class MasteryDataExtractorUnitTest extends AbstractExtractorUnitTest<MasteryData> {

    @Override
    protected IExtractor<MasteryData> createExtractor() {
        return new MasteryDataExtractor();
    }

    @Test
    @DisplayName("Should extract mastery data from CSV")
    void testExtractMasteryData() {
        // Format: masteryId, level, name, requiredSP
        String content = "101\t5\tSword Mastery\t1000";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("skillmasterydata.txt", content)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one mastery");

        MasteryData mastery = testListener.getExtractedItems().get(0);
        assertNotNull(mastery);
        assertEquals(101, mastery.getMasteryId());
        assertEquals(5, mastery.getLevel());
        assertEquals(1000, mastery.getRequiredSp());
    }

    @Test
    @DisplayName("Should skip entries with xxx in name")
    void testSkipXxxEntries() {
        StringBuilder sb = new StringBuilder();
        sb.append("101\t5\txxx_unused\t1000\n");
        sb.append("102\t10\tValid Mastery\t2000");

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("skillmasterydata.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError());
        assertEquals(1, testListener.getExtractedItems().size());
        assertEquals(102, testListener.getExtractedItems().get(0).getMasteryId());
    }

    @Test
    @DisplayName("Should report error on missing file")
    void testMissingFile() {
        IPk2Driver driver = new MockPk2DriverBuilder().build();

        extractor.extract(driver, testListener, testProgressListener);

        // MasteryDataExtractor throws Pk2MissedResourceException when file is not found
        assertTrue(testListener.hasError(), "Should report error for missing file");
    }
}
