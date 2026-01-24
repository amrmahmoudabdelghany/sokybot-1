package org.sokybot.pk2extractor.mediapk2.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.sokybot.pk2.JMXFile;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.item.ItemData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class ItemDataExtractorUnitTest extends AbstractExtractorUnitTest<ItemData> {

    @Override
    protected IExtractor<ItemData> createExtractor() {
        return new ItemDataExtractor();
    }

    @Test
    void testExtractItemData() {
        // Construct a sample CSV line with enough columns
        // We need at least 125 columns based on the extractor code
        List<String> columns = IntStream.range(0, 130)
                .mapToObj(String::valueOf)
                .collect(Collectors.toList());

        // Set specific values for fields we want to test
        columns.set(1, "12345"); // refId
        columns.set(2, "ITEM_TEST_SWORD"); // longId
        columns.set(5, "Test Sword"); // name
        columns.set(7, "1"); // isMallItem
        
        // Type
        columns.set(9, "3"); // type1
        columns.set(10, "1"); // type2
        columns.set(11, "6"); // type3
        columns.set(12, "2"); // type4
        
        columns.set(15, "2"); // rarity
        
        // Flags
        columns.set(16, "1"); // canTrade
        columns.set(17, "1"); // canSell
        columns.set(18, "1"); // canBuy
        columns.set(20, "1"); // canDrop
        columns.set(22, "1"); // canRepair
        columns.set(24, "1"); // canUse
        
        columns.set(26, "1000"); // price
        columns.set(31, "500"); // sellPrice
        columns.set(33, "10"); // requiredLevel
        columns.set(54, "icon/sword.ddj"); // iconPath
        columns.set(58, "1"); // biologicalType
        columns.set(94, "5"); // range
        
        // Params
        columns.set(118, "100");
        columns.set(120, "200");
        columns.set(122, "300");
        columns.set(124, "400");

        String csvContent = String.join("\t", columns);

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("itemdata_5000.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        when(mockDriver.find(anyString())).thenReturn(Collections.singletonList(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<ItemData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        ItemData item = items.get(0);
        assertEquals(12345, item.getRefId());
        assertEquals("ITEM_TEST_SWORD", item.getLongId());
        assertEquals("Test Sword", item.getName());
        assertTrue(item.isMallItem());
        
        assertEquals(3, item.getItemType().getType1());
        assertEquals(1, item.getItemType().getType2());
        assertEquals(6, item.getItemType().getType3());
        assertEquals(2, item.getItemType().getType4());
        
        assertEquals((byte)2, item.getRarity());
        assertTrue(item.isCanTrade());
        assertTrue(item.isCanSell());
        assertTrue(item.isCanBuy());
        assertTrue(item.isCanDrop());
        assertTrue(item.isCanRepair());
        assertTrue(item.isCanUse());
        
        assertEquals(1000, item.getPrice());
        assertEquals(500, item.getSellPrice());
        assertEquals(10, item.getRequiredLevel());
        assertEquals("icon/sword.ddj", item.getIconPath());
        assertEquals(1, item.getBiologicalType());
        assertEquals(5, item.getRange());
        
        int[] params = item.getParams();
        assertNotNull(params);
        assertEquals(4, params.length);
        assertEquals(100, params[0]);
        assertEquals(200, params[1]);
        assertEquals(300, params[2]);
        assertEquals(400, params[3]);
    }
}
