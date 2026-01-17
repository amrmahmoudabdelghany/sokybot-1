package org.sokybot.pk2extractor.mediapk2.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.progression.LevelGoldData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for LevelGoldExtractor.
 */
@DisplayName("LevelGoldExtractor Unit Tests")
class LevelGoldExtractorUnitTest extends AbstractExtractorUnitTest<LevelGoldData> {

    @Override
    protected IExtractor<LevelGoldData> createExtractor() {
        return new LevelGoldExtractor();
    }

    @Test
    @DisplayName("Should extract level gold data from CSV")
    void testExtractLevelGoldData() {
        // Format: level, minGold, maxGold
        String content = "1\t100\t500\n10\t1000\t5000";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("levelgold.txt", content, StandardCharsets.UTF_16)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(2, testListener.getExtractedItems().size(), "Should extract two entries");

        LevelGoldData gold1 = testListener.getExtractedItems().get(0);
        assertNotNull(gold1);
        assertEquals(1, gold1.getLevel());
        assertEquals(100, gold1.getMinGold());
        assertEquals(500, gold1.getMaxGold());

        LevelGoldData gold10 = testListener.getExtractedItems().get(1);
        assertEquals(10, gold10.getLevel());
        assertEquals(1000, gold10.getMinGold());
        assertEquals(5000, gold10.getMaxGold());
    }

    @Test
    @DisplayName("Should skip lines with fewer than 3 columns")
    void testSkipShortLines() {
        String content = "1\t100\n10\t1000\t5000";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("levelgold.txt", content, StandardCharsets.UTF_16)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError());
        assertEquals(1, testListener.getExtractedItems().size());
    }

    @Test
    @DisplayName("Should report error on missing file")
    void testMissingFile() {
        IPk2Driver driver = new MockPk2DriverBuilder().build();

        extractor.extract(driver, testListener, testProgressListener);

        // LevelGoldExtractor throws Pk2MissedResourceException when file is not found
        assertTrue(testListener.hasError(), "Should report error for missing file");
    }
}
