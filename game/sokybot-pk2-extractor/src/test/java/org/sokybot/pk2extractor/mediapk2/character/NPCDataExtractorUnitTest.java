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
import org.sokybot.pk2extractor.dto.character.NPCData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for NPCDataExtractor.
 */
@DisplayName("NPCDataExtractor Unit Tests")
class NPCDataExtractorUnitTest extends AbstractExtractorUnitTest<NPCData> {

    @Override
    protected IExtractor<NPCData> createExtractor() {
        return new NPCDataExtractor();
    }

    @Test
    @DisplayName("Should extract NPC data correctly")
    void testExtractNPCData() {
        // Prepare mock data
        String indexFileContent = "npc_data.txt";
        
        // Construct a representative CSV line mimicking the real file structure
        // We ensure we provide values for the indices used in NPCDataExtractor
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60; i++) {
             // Defaults
            if (i == 1) sb.append("12345"); // refId
            else if (i == 2) sb.append("NPC_TEST_MOB"); // longId
            else if (i == 5) sb.append("Test Monster"); // name
            else if (i == 10) sb.append("1"); // type flag part
            else if (i == 11) sb.append("0"); // type flag part
            else if (i == 12) sb.append("0"); // type flag part
            else if (i == 14) sb.append("0"); // type flag part
            else if (i == 15) sb.append("0"); // type flag part (combines to 10000)
            else if (i == 57) sb.append("10"); // level
            else if (i == 59) sb.append("5000"); // hp
            else sb.append("0");
            
            sb.append("\t");
        }
        
        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("characterdata.txt", indexFileContent, StandardCharsets.UTF_16)
                .withFile("npc_data.txt", sb.toString(), StandardCharsets.UTF_16)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one item");
        
        NPCData npc = testListener.getExtractedItems().get(0);
        assertNotNull(npc);
        assertEquals(12345, npc.getRefId());
        assertEquals("NPC_TEST_MOB", npc.getLongId());
        assertEquals("Test Monster", npc.getName());
        assertEquals(10, npc.getLevel());
        assertEquals(5000, npc.getHP());
    }

    @Test
    @DisplayName("Should handle missing index file gracefully")
    void testMissingIndexFile() {
        IPk2Driver driver = new MockPk2DriverBuilder().build();

        extractor.extract(driver, testListener, testProgressListener);

        // The extractor catches exceptions and reports them via onError
        // Since the index file is missing, it throws Pk2MissedResourceException inside, caught and passed to listener
        assertTrue(testListener.hasError(), "Should report error for missing index file");
    }

    @Test
    @DisplayName("Should handle empty index file")
    void testEmptyIndexFile() {
         IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("characterdata.txt", "", StandardCharsets.UTF_16)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Should not fail on empty index file");
        assertEquals(0, testListener.getExtractedItems().size(), "Should not extract any items");
    }
}
