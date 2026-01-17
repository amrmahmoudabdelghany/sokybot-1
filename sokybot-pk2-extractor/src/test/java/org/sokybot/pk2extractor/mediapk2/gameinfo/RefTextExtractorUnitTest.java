package org.sokybot.pk2extractor.mediapk2.gameinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.common.RefTextData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for RefTextExtractor.
 */
@DisplayName("RefTextExtractor Unit Tests")
class RefTextExtractorUnitTest extends AbstractExtractorUnitTest<RefTextData> {

    @Override
    protected IExtractor<RefTextData> createExtractor() {
        return new RefTextExtractor();
    }

    @Test
    @DisplayName("Should extract localized text data")
    void testExtractRefTextData() {
        // Format: service, key, data...
        // Typical format: 1\tSN_KEY\tLocalized Text\t\t\t
        String content = "1\tSN_TEST_KEY\tTest Message";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("textdata_object.txt", content)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one item");

        RefTextData text = testListener.getExtractedItems().get(0);
        assertNotNull(text);
        assertEquals("SN_TEST_KEY", text.getNameStrId());
        assertEquals("Test Message", text.getData());
    }

    @Test
    @DisplayName("Should handle alternate key index if column 2 starts with SN_")
    void testAlternateKeyIndex() {
        // Format where column 2 is the key
        String content = "1\tSOME_FLAG\tSN_ALT_KEY\tAlt Message";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("textdata_equip.txt", content)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError());
        assertEquals(1, testListener.getExtractedItems().size());

        RefTextData text = testListener.getExtractedItems().get(0);
        assertEquals("SN_ALT_KEY", text.getNameStrId());
        assertEquals("Alt Message", text.getData());
    }
    
    @Test
    @DisplayName("Should handle multiple empty trailing columns")
    void testTrailingEmptyColumns() {
        // Data usually in the last non-empty column
        String content = "1\tSN_KEY\tReal Data\t\t\t\t";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("textdata_ui.txt", content)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertEquals(1, testListener.getExtractedItems().size());
        assertEquals("Real Data", testListener.getExtractedItems().get(0).getData());
    }

    @Test
    @DisplayName("Should skip short lines")
    void testSkipShortLines() {
        String content = "1\tSN_KEY"; // Too short

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("textdata_err.txt", content)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertEquals(0, testListener.getExtractedItems().size());
    }
}
