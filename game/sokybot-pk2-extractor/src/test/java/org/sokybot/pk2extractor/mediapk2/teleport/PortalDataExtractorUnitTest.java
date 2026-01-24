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
import org.sokybot.pk2extractor.dto.teleport.PortalData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class PortalDataExtractorUnitTest extends AbstractExtractorUnitTest<PortalData> {

    @Override
    protected IExtractor<PortalData> createExtractor() {
        return new PortalDataExtractor();
    }

    @Test
    void testExtractPortalData() throws Exception {
        // Columns: 0(dummy), 1(refId), 2(longId), 3, 4, 5(name)
        String csvContent = String.join("\t",
            "0",                   // 0: dummy
            "1001",                // 1: refId
            "PORTAL_JANGAN",       // 2: longId
            "0",                   // 3: unused
            "0",                   // 4: unused
            "Jangan Portal"        // 5: name
        );

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("teleportbuilding.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16LE));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        when(mockDriver.find(anyString())).thenReturn(Collections.singletonList(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<PortalData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        PortalData item = items.get(0);
        assertEquals(1001, item.getRefId());
        assertEquals("PORTAL_JANGAN", item.getLongId());
        assertEquals("Jangan Portal", item.getName());
    }
}
