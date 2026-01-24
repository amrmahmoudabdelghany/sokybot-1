package org.sokybot.pk2extractor.mediapk2.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.shop.RefShopTabData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;
import org.sokybot.pk2extractor.test.util.MockPk2DriverBuilder;

/**
 * Unit tests for RefShopTabExtractor.
 */
@DisplayName("RefShopTabExtractor Unit Tests")
class RefShopTabExtractorUnitTest extends AbstractExtractorUnitTest<RefShopTabData> {

    @Override
    protected IExtractor<RefShopTabData> createExtractor() {
        return new RefShopTabExtractor();
    }

    @Test
    @DisplayName("Should extract shop tab data")
    void testExtractShopTab() {
        // Format: service, country, id, codeName, refGroupCode, strID
        String content = "1\t15\t100\tSTORE_POTION_TAB\tSTORE_POTION_GROUP\tSN_STORE_POTION";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("refshoptab.txt", content)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertFalse(testListener.hasError(), "Extraction should not fail");
        assertEquals(1, testListener.getExtractedItems().size(), "Should extract one tab");

        RefShopTabData tab = testListener.getExtractedItems().get(0);
        assertNotNull(tab);
        assertEquals(1, tab.getService());
        assertEquals(15, tab.getCountry());
        assertEquals(100, tab.getId());
        assertEquals("STORE_POTION_TAB", tab.getCodeName());
        assertEquals("STORE_POTION_GROUP", tab.getRefTabGroupCodeName());
        assertEquals("SN_STORE_POTION", tab.getStrID128_Tab());
    }

    @Test
    @DisplayName("Should skip short lines")
    void testSkipShortLines() {
        String content = "1\t15\t100\tTOO_SHORT";

        IPk2Driver driver = new MockPk2DriverBuilder()
                .withFile("refshoptab.txt", content)
                .build();

        extractor.extract(driver, testListener, testProgressListener);

        assertEquals(0, testListener.getExtractedItems().size());
    }
}
