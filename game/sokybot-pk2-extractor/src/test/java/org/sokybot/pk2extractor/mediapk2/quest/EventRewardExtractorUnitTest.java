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
import org.sokybot.pk2extractor.dto.quest.EventRewardData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class EventRewardExtractorUnitTest extends AbstractExtractorUnitTest<EventRewardData> {

    @Override
    protected IExtractor<EventRewardData> createExtractor() {
        return new EventRewardExtractor();
    }

    @Test
    void testExtractEventReward() throws Exception {
        // Columns: 0(eventId), 1(eventCodeName), 2(itemCodeName), 3(itemAmount),
        // 4-6 unused, 7(minRequiredLevel), 8(maxRequiredLevel)
        String csvContent = String.join("\t",
            "501",                  // 0: eventId
            "EVENT_HALLOWEEN",      // 1: eventCodeName
            "ITEM_CANDY",           // 2: itemCodeName
            "10",                   // 3: itemAmount
            "0", "0", "0",          // 4-6: unused
            "1",                    // 7: minRequiredLevel
            "80"                    // 8: maxRequiredLevel
        );

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("refeventrewarditems.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16LE));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        when(mockDriver.find(anyString())).thenReturn(Collections.singletonList(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<EventRewardData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        EventRewardData item = items.get(0);
        assertEquals(501, item.getEventId());
        assertEquals("EVENT_HALLOWEEN", item.getEventCodeName());
        assertEquals("ITEM_CANDY", item.getItemCodeName());
        assertEquals(10, item.getItemAmount());
        assertEquals(1, item.getMinRequiredLevel());
        assertEquals(80, item.getMaxRequiredLevel());
    }
}
