package org.sokybot.pk2extractor.mediapk2.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.sokybot.pk2extractor.dto.quest.QuestRewardData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class QuestRewardExtractorUnitTest extends AbstractExtractorUnitTest<QuestRewardData> {

    @Override
    protected IExtractor<QuestRewardData> createExtractor() {
        return new QuestRewardExtractor();
    }

    @Test
    void testExtractQuestReward() throws Exception {
        // Columns: 0(questId), 1(questCodeName), 2(isView), 3, 4(isItemReward),
        // 5-9 unused, 10(gold), 11(exp), 12(spExp), 13(sp), 14(ap), 15(apType), 16(hwan), 17(inventorySlots), 18(itemRewardType)
        String csvContent = String.join("\t",
            "1001",                 // 0: questId
            "QUEST_JANGAN_01",      // 1: questCodeName
            "1",                    // 2: isView
            "0",                    // 3: unused
            "1",                    // 4: isItemReward
            "0", "0", "0", "0", "0",// 5-9: unused
            "50000",                // 10: gold
            "10000",                // 11: exp
            "500",                  // 12: spExp
            "100",                  // 13: sp
            "20",                   // 14: ap
            "AP_NORMAL",            // 15: apType
            "5",                    // 16: hwan
            "2",                    // 17: inventorySlots
            "1"                     // 18: itemRewardType
        );

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("refquestreward.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16LE));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        when(mockDriver.find(anyString())).thenReturn(Collections.singletonList(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<QuestRewardData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        QuestRewardData item = items.get(0);
        assertEquals(1001, item.getQuestId());
        assertEquals("QUEST_JANGAN_01", item.getQuestCodeName());
        assertTrue(item.isView());
        assertTrue(item.isItemReward());
        assertEquals(50000, item.getGold());
        assertEquals(10000, item.getExp());
        assertEquals(500, item.getSpExp());
        assertEquals(100, item.getSp());
        assertEquals(20, item.getAp());
        assertEquals("AP_NORMAL", item.getApType());
        assertEquals(5, item.getHwan());
        assertEquals(2, item.getInventorySlots());
        assertEquals(1, item.getItemRewardType());
    }
}
