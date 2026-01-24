package org.sokybot.pk2extractor.mediapk2.teleport;

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
import org.sokybot.pk2extractor.dto.teleport.TeleportLinkData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class TeleportLinkExtractorUnitTest extends AbstractExtractorUnitTest<TeleportLinkData> {

    @Override
    protected IExtractor<TeleportLinkData> createExtractor() {
        return new TeleportLinkExtractor();
    }

    @Test
    void testExtractTeleportLink() throws Exception {
        // Construct a sample CSV line with Tab delimiter
        // Columns (based on TeleportLinkExtractor):
        // 0: dummy, 1: ownerTeleportId, 2: targetTeleportId, 3: fee
        // 4: restrictBindMethod, 5: checkResult, 6: restrict1, 7: data1_1, 8: data1_2
        // 9: restrict2, 10: data2_1, 11: data2_2
        String csvContent = String.join("\t",
            "0",     // 0: dummy
            "101",   // 1: ownerTeleportId
            "202",   // 2: targetTeleportId
            "5000",  // 3: fee
            "1",     // 4: restrictBindMethod
            "2",     // 5: checkResult
            "10",    // 6: restrict1
            "20",    // 7: data1_1
            "30",    // 8: data1_2
            "40",    // 9: restrict2
            "50",    // 10: data2_1
            "60"     // 11: data2_2
        );

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("teleportlink.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16LE));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        when(mockDriver.find(anyString())).thenReturn(Collections.singletonList(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<TeleportLinkData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        TeleportLinkData item = items.get(0);
        assertEquals(101, item.getOwnerTeleportId());
        assertEquals(202, item.getTargetTeleportId());
        assertEquals(5000, item.getFee());
        assertEquals(1, item.getRestrictBindMethod());
        assertEquals(2, item.getCheckResult());
        assertEquals(10, item.getRestrict1());
        assertEquals(20, item.getData1_1());
        assertEquals(30, item.getData1_2());
        assertEquals(40, item.getRestrict2());
        assertEquals(50, item.getData2_1());
        assertEquals(60, item.getData2_2());
    }
}
