package org.sokybot.pk2extractor.mediapk2.quest;

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
import org.sokybot.pk2extractor.dto.quest.QuestRewardItemData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class QuestRewardItemExtractorUnitTest extends AbstractExtractorUnitTest<QuestRewardItemData> {

    @Override
    protected IExtractor<QuestRewardItemData> createExtractor() {
        return new QuestRewardItemExtractor();
    }

    @Test
    void testExtractQuestRewardItem() throws Exception {
        // Columns: 0(questId), 1(questCodeName), 2(rewardType), 3(itemCodeName),
        // 4(optionalItemCode), 5(optionalItemCount), 6(achieveQuantity), 7(rentItemCodeName)
        String csvContent = String.join("\t",
            "1001",                 // 0: questId
            "QUEST_JANGAN_01",      // 1: questCodeName
            "1",                    // 2: rewardType
            "ITEM_SWORD_01",        // 3: itemCodeName
            "ITEM_SHIELD_01",       // 4: optionalItemCode
            "5",                    // 5: optionalItemCount
            "10",                   // 6: achieveQuantity
            "ITEM_RENT_ARMOR"       // 7: rentItemCodeName
        );

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("refquestrewarditem.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16LE));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        when(mockDriver.find(anyString())).thenReturn(Collections.singletonList(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<QuestRewardItemData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        QuestRewardItemData item = items.get(0);
        assertEquals(1001, item.getQuestId());
        assertEquals("QUEST_JANGAN_01", item.getQuestCodeName());
        assertEquals(1, item.getRewardType());
        assertEquals("ITEM_SWORD_01", item.getItemCodeName());
        assertEquals("ITEM_SHIELD_01", item.getOptionalItemCode());
        assertEquals(5, item.getOptionalItemCount());
        assertEquals(10, item.getAchieveQuantity());
        assertEquals("ITEM_RENT_ARMOR", item.getRentItemCodeName());
    }
}
