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
import org.sokybot.pk2extractor.dto.teleport.OptionalTeleportData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class OptionalTeleportExtractorUnitTest extends AbstractExtractorUnitTest<OptionalTeleportData> {

    @Override
    protected IExtractor<OptionalTeleportData> createExtractor() {
        return new OptionalTeleportExtractor();
    }

    @Test
    void testExtractOptionalTeleport() throws Exception {
        // Columns: 0(dummy), 1(id), 2(objName128), 3(zoneName128), 4(regionId),
        // 5(posX), 6(posZ), 7(posY), 8(worldId), 9, 10, 11(minLevel), 12(maxLevel)
        String csvContent = String.join("\t",
            "0",                    // 0: dummy
            "501",                  // 1: id
            "OBJ_TELEPORT_JN",      // 2: objName128
            "ZONE_JANGAN",          // 3: zoneName128
            "25000",                // 4: regionId
            "100.5",                // 5: posX
            "200.3",                // 6: posZ
            "50.1",                 // 7: posY
            "1",                    // 8: worldId
            "0",                    // 9: unused
            "0",                    // 10: unused
            "10",                   // 11: minLevel
            "80"                    // 12: maxLevel
        );

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("refoptionalteleport.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16LE));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        when(mockDriver.find(anyString())).thenReturn(Collections.singletonList(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<OptionalTeleportData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        OptionalTeleportData item = items.get(0);
        assertEquals(501, item.getId());
        assertEquals("OBJ_TELEPORT_JN", item.getObjName128());
        assertEquals("ZONE_JANGAN", item.getZoneName128());
        assertEquals(25000, item.getRegionId());
        assertEquals(100.5f, item.getPosX(), 0.01f);
        assertEquals(200.3f, item.getPosZ(), 0.01f);
        assertEquals(50.1f, item.getPosY(), 0.01f);
        assertEquals(1, item.getWorldId());
        assertEquals(10, item.getMinLevel());
        assertEquals(80, item.getMaxLevel());
    }
}
