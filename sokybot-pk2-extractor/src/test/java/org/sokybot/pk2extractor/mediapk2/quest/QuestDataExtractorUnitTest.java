package org.sokybot.pk2extractor.mediapk2.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.quest.QuestData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for QuestDataExtractor.
 */
@DisplayName("QuestDataExtractor Unit Tests")
class QuestDataExtractorUnitTest extends AbstractExtractorUnitTest<QuestData> {

    @Override
    protected IExtractor<QuestData> createExtractor() {
        return new QuestDataExtractor();
    }

    @Test
    @DisplayName("Should extract quest data from CSV")
    void testExtractQuestData() {
        // Format: service, id, longId, name, type, minLvl, maxLvl, repeat, party, npcRef, ..., rewardExp, rewardGold, rewardSP
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            if (i == 1) sb.append("100"); // id
            else if (i == 2) sb.append("QUEST_TEST"); // longId
            else if (i == 3) sb.append("Test Quest"); // name
            else if (i == 4) sb.append("1"); // type
            else if (i == 5) sb.append("10"); // minLevel
            else if (i == 6) sb.append("50"); // maxLevel
            else if (i == 15) sb.append("10000"); // rewardExp
            else if (i == 16) sb.append("5000"); // rewardGold
            else sb.append("0");
            sb.append("\t");
        }

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("questdata.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one quest");

        QuestData quest = testListener.getExtractedItems().get(0);
        assertNotNull(quest);
        assertEquals(100, quest.getId());
        assertEquals("QUEST_TEST", quest.getLongId());
        assertEquals("Test Quest", quest.getName());
        assertEquals(10, quest.getMinLevel());
        assertEquals(50, quest.getMaxLevel());
        assertEquals(10000, quest.getRewardExp());
        assertEquals(5000, quest.getRewardGold());
    }

    @Test
    @DisplayName("Should skip lines with fewer than 21 columns")
    void testSkipShortLines() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            sb.append("0\t");
        }

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("questdata.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError());
        assertEquals(0, testListener.getExtractedItems().size());
    }

    @Test
    @DisplayName("Should skip comment lines")
    void testSkipComments() {
        StringBuilder sb = new StringBuilder();
        sb.append("// This is a comment\n");
        for (int i = 0; i < 25; i++) {
            if (i == 1) sb.append("100");
            else sb.append("0");
            sb.append("\t");
        }

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("questdata.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError());
        assertEquals(1, testListener.getExtractedItems().size());
    }

    @Test
    @DisplayName("Should handle missing file gracefully")
    void testMissingFile() {
        IPk2Driver driver = new MockPk2DriverBuilder().build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError());
        assertEquals(0, testListener.getExtractedItems().size());
    }
}
