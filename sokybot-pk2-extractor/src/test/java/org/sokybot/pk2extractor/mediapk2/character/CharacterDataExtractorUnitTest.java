package org.sokybot.pk2extractor.mediapk2.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.character.CharacterData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for CharacterDataExtractor.
 */
@DisplayName("CharacterDataExtractor Unit Tests")
class CharacterDataExtractorUnitTest extends AbstractExtractorUnitTest<CharacterData> {

    @Override
    protected IExtractor<CharacterData> createExtractor() {
        return new CharacterDataExtractor();
    }

    @Test
    @DisplayName("Should extract character data correctly")
    void testExtractCharacterData() {
        // Needs characterdata.txt as index, and then the actual file
        String indexContent = "npc_data.txt";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            if (i == 1) sb.append("1900"); // RefID
            else if (i == 2) sb.append("MOB_TEST_01"); // CodeName
            else if (i == 5) sb.append("Test Monster"); // Name
            else if (i == 57) sb.append("10"); // Level
            else if (i == 59) sb.append("1000"); // HP
            else if (i == 60) sb.append("500"); // MP
            else sb.append("0");
            sb.append("\t");
        }

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("characterdata.txt", indexContent, StandardCharsets.UTF_16)
                .withFile("npc_data.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one character");

        CharacterData charData = testListener.getExtractedItems().get(0);
        assertNotNull(charData);
        assertEquals(1900, charData.getRefId());
        assertEquals("MOB_TEST_01", charData.getLongId());
        assertEquals("Test Monster", charData.getName());
        assertEquals(10, charData.getLevel());
        assertEquals(1000, charData.getMaxHP());
    }

    @Test
    @DisplayName("Should extract from teleportbuilding.txt as well")
    void testExtractTeleportBuilding() {
        // teleportbuilding.txt is read directly
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            if (i == 1) sb.append("2000"); // RefID
            else if (i == 2) sb.append("NPC_TELEPORT"); // CodeName
            else sb.append("0");
            sb.append("\t");
        }

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("characterdata.txt", "", StandardCharsets.UTF_16) // Empty index
                .withFile("teleportbuilding.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError());
        assertEquals(1, testListener.getExtractedItems().size());
        assertEquals(2000, testListener.getExtractedItems().get(0).getRefId());
    }

    @Test
    @DisplayName("Should handle missing index file")
    void testMissingIndexFile() {
        IPk2Driver driver = new MockPk2DriverBuilder().build();

        extractor.extract(driver, testListener, testProgressListener);

        // Should report error because characterdata.txt is missing
        assertTrue(testListener.hasError());
    }
}
