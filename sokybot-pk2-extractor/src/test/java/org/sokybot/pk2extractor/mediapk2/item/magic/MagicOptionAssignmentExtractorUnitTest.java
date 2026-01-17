package org.sokybot.pk2extractor.mediapk2.item.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.item.magic.MagicOptionAssignmentData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for MagicOptionAssignmentExtractor.
 */
@DisplayName("MagicOptionAssignmentExtractor Unit Tests")
class MagicOptionAssignmentExtractorUnitTest extends AbstractExtractorUnitTest<MagicOptionAssignmentData> {

    @Override
    protected IExtractor<MagicOptionAssignmentData> createExtractor() {
        return new MagicOptionAssignmentExtractor();
    }

    @Test
    @DisplayName("Should extract magic option assignment")
    void testExtractMagicAssignment() {
        // Format: service, race, type3, type4, options...
        String content = "1\t3\t10\t5\tMATTR_STR\tMATTR_INT\txxx\tNULL\tMATTR_DUR";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("refmagicoptassign.txt", content)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one assignment");

        MagicOptionAssignmentData assign = testListener.getExtractedItems().get(0);
        assertNotNull(assign);
        assertEquals((byte)3, assign.getRace());
        assertEquals((byte)10, assign.getTypeId3());
        assertEquals((byte)5, assign.getTypeId4());
        assertEquals(3, assign.getAvailableMagicOptions().size());
        assertTrue(assign.getAvailableMagicOptions().contains("MATTR_STR"));
        assertTrue(assign.getAvailableMagicOptions().contains("MATTR_INT"));
        assertTrue(assign.getAvailableMagicOptions().contains("MATTR_DUR"));
    }

    @Test
    @DisplayName("Should filter xxx and NULL options")
    void testFilterInvalidOptions() {
        String content = "1\t0\t0\t0\txxx\tNULL";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("refmagicoptassign.txt", content)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        // Should skip because race/type are 0 and options are empty after filtering
        assertEquals(0, testListener.getExtractedItems().size());
    }
}
