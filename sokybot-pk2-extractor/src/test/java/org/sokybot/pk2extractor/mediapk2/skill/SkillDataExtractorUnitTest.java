package org.sokybot.pk2extractor.mediapk2.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.skill.SkillData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for SkillDataExtractor.
 */
@DisplayName("SkillDataExtractor Unit Tests")
class SkillDataExtractorUnitTest extends AbstractExtractorUnitTest<SkillData> {

    @Override
    protected IExtractor<SkillData> createExtractor() {
        return new SkillDataExtractor();
    }

    @Test
    @DisplayName("Should extract skill data from CSV")
    void testExtractSkillData() {
        // Build a mock CSV with 71+ columns as required by extractor filter
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 120; i++) {
            if (i == 1) sb.append("1001"); // refId
            else if (i == 3) sb.append("SKILL_TEST_001"); // longId
            else if (i == 11) sb.append("100"); // preparingTime
            else if (i == 12) sb.append("500"); // castTime
            else if (i == 13) sb.append("3000"); // duration
            else if (i == 14) sb.append("10000"); // cooldown
            else if (i == 53) sb.append("50"); // MP cost
            else if (i == 62) sb.append("Test Skill"); // name
            else sb.append("0");
            sb.append("\t");
        }

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("skilldata_0.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one skill");

        SkillData skill = testListener.getExtractedItems().get(0);
        assertNotNull(skill);
        assertEquals(1001, skill.getRefId());
        assertEquals("SKILL_TEST_001", skill.getLongId());
        assertEquals("Test Skill", skill.getName());
        assertEquals(50, skill.getMP());
    }

    @Test
    @DisplayName("Should skip lines with fewer than 70 columns")
    void testSkipShortLines() {
        // Build a line with only 50 columns
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("0\t");
        }

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("skilldata_1.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Should not fail");
        assertEquals(0, testListener.getExtractedItems().size(), "Should not extract skills from short line");
    }

    @Test
    @DisplayName("Should skip comment lines")
    void testSkipComments() {
        StringBuilder sb = new StringBuilder();
        sb.append("// This is a comment line\n");
        for (int i = 0; i < 120; i++) {
            if (i == 1) sb.append("1001");
            else sb.append("0");
            sb.append("\t");
        }

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("skilldata_0.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError());
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract valid skill but skip comment");
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
