package org.sokybot.pk2extractor.mediapk2.item.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.item.magic.MagicOptionData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for MagicOptionExtractor.
 */
@DisplayName("MagicOptionExtractor Unit Tests")
class MagicOptionExtractorUnitTest extends AbstractExtractorUnitTest<MagicOptionData> {

    @Override
    protected IExtractor<MagicOptionData> createExtractor() {
        return new MagicOptionExtractor();
    }

    @Test
    @DisplayName("Should extract magic option data")
    void testExtractMagicOption() {
        // Format: service, id, longId, name, attrType, min, max, degree, weapon, armor, acc, shield
        StringBuilder sb = new StringBuilder();
        sb.append("1\t100\tMATTR_STR\tSTR Increase\t1\t1\t5\t1\t1\t0\t0\t0");

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("magicoption.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one option");

        MagicOptionData option = testListener.getExtractedItems().get(0);
        assertNotNull(option);
        assertEquals(100, option.getId());
        assertEquals("MATTR_STR", option.getLongId());
        assertEquals("STR Increase", option.getName());
        assertEquals(1, option.getAttributeType());
        assertEquals(1, option.getMinValue());
        assertEquals(5, option.getMaxValue());
        assertTrue(option.isWeapon());
        assertFalse(option.isArmor());
    }

    @Test
    @DisplayName("Should skip short lines")
    void testSkipShortLines() {
        String content = "1\t100\tSHORT\n";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("magicoption.txt", content)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertEquals(0, testListener.getExtractedItems().size());
    }

    @Test
    @DisplayName("Should skip comments")
    void testSkipComments() {
        StringBuilder sb = new StringBuilder();
        sb.append("// Comment\n");
        sb.append("1\t100\tMATTR_VALID\tName\t1\t1\t5\t1\t1\t0\t0\t0");

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("magicoption.txt", sb.toString())
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError());
        assertEquals(1, testListener.getExtractedItems().size());
    }
}
