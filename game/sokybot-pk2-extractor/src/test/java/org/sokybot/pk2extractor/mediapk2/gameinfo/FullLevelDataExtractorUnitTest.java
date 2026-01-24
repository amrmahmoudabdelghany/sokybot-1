package org.sokybot.pk2extractor.mediapk2.gameinfo;

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
import org.sokybot.pk2extractor.dto.progression.FullLevelData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class FullLevelDataExtractorUnitTest extends AbstractExtractorUnitTest<FullLevelData> {

    @Override
    protected IExtractor<FullLevelData> createExtractor() {
        return new FullLevelDataExtractor();
    }

    @Test
    void testExtractFullLevelData() throws Exception {
        // Columns: 0(Level), 1(PlayerExp), 2(MasteryExp), 3-8, 9(PetExp), 10(PetStoredSp)
        String csvContent = String.join("\t",
            "10",                   // 0: Level
            "1000",                 // 1: PlayerExp
            "500",                  // 2: MasteryExp
            "0", "0", "0", "0", "0", "0", // 3-8: unused
            "2000",                 // 9: PetExp
            "100"                   // 10: PetStoredSp
        );

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("leveldata.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16LE));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        when(mockDriver.find(anyString())).thenReturn(Collections.singletonList(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<FullLevelData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        FullLevelData item = items.get(0);
        assertEquals(10, item.getLevel());
        assertEquals(1000L, item.getPlayerExp());
        assertEquals(500, item.getMasteryExp());
        assertEquals(2000L, item.getPetExp());
        assertEquals(100, item.getPetStoredSp());
    }
}
