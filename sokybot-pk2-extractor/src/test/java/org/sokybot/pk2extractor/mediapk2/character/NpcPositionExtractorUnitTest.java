package org.sokybot.pk2extractor.mediapk2.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sokybot.pk2.JMXFile;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.character.NpcPositionData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class NpcPositionExtractorUnitTest extends AbstractExtractorUnitTest<NpcPositionData> {

    @Override
    protected IExtractor<NpcPositionData> createExtractor() {
        return new NpcPositionExtractor();
    }

    @Test
    void testExtractNpcPosition() throws Exception {
        // Columns: 0(npcId), 1(region), 2(x), 3(y), 4(z)
        String csvContent = String.join("\t",
            "1001",     // 0: npcId
            "25000",    // 1: region
            "100.5",    // 2: x
            "200.3",    // 3: y
            "50.1"      // 4: z
        );

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("npcpos.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        when(mockDriver.findFirst(anyString())).thenReturn(Optional.of(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<NpcPositionData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        NpcPositionData item = items.get(0);
        assertEquals(1001, item.getNpcId());
        assertEquals(25000, item.getRegion());
        assertEquals(100.5f, item.getX(), 0.01f);
        assertEquals(200.3f, item.getY(), 0.01f);
        assertEquals(50.1f, item.getZ(), 0.01f);
    }
}
