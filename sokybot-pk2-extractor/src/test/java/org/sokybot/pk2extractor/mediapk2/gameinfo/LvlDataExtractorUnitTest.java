package org.sokybot.pk2extractor.mediapk2.gameinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sokybot.pk2.JMXFile;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.progression.LvlData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class LvlDataExtractorUnitTest extends AbstractExtractorUnitTest<LvlData> {

    @Override
    protected IExtractor<LvlData> createExtractor() {
        return new LvlDataExtractor();
    }

    @Test
    void testExtractLvlData() throws Exception {
        // Columns: 0(Level), 1(Exp)
        String csvContent = "20\t5000";

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("leveldata.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        // LvlDataExtractor uses driver.findFirst() instead of driver.find()
        when(mockDriver.findFirst(anyString())).thenReturn(Optional.of(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<LvlData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        LvlData item = items.get(0);
        assertEquals(20, item.getLevel());
        assertEquals(5000L, item.getExperience());
    }
}
