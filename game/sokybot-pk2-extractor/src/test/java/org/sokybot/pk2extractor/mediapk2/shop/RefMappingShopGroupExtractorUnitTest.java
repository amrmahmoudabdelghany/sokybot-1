package org.sokybot.pk2extractor.mediapk2.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sokybot.pk2.JMXFile;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.shop.RefMappingShopGroupData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class RefMappingShopGroupExtractorUnitTest extends AbstractExtractorUnitTest<RefMappingShopGroupData> {

    @Override
    protected IExtractor<RefMappingShopGroupData> createExtractor() {
        return new RefMappingShopGroupExtractor();
    }

    @Test
    void testExtractMapping() {
        // Construct a sample CSV line with Tab delimiter
        // Column 1: country (int)
        // Column 2: groupCodeName (String)
        // Column 3: shopCodeName (String)
        // We need padding to match index behavior (index 1 is country).
        // Indices: 0 (dummy), 1 (country), 2 (group), 3 (shop)
        String csvContent = "0\t1\tGROUP_TEST\tSHOP_TEST";

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("refmappingshopgroup.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16LE));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        when(mockDriver.find(anyString())).thenReturn(Collections.singletonList(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<RefMappingShopGroupData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        RefMappingShopGroupData item = items.get(0);
        assertEquals(1, item.getCountry());
        assertEquals("GROUP_TEST", item.getGroupCodeName());
        assertEquals("SHOP_TEST", item.getShopCodeName());
    }
}
