package org.sokybot.pk2extractor.mediapk2.item;

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
import org.sokybot.pk2extractor.dto.item.PackageItemData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class PackageItemExtractorUnitTest extends AbstractExtractorUnitTest<PackageItemData> {

    @Override
    protected IExtractor<PackageItemData> createExtractor() {
        return new PackageItemExtractor();
    }

    @Test
    void testExtractPackageItem() throws Exception {
        // Columns: 0-1 unused, 2(packageCodeName), 3(itemCodeName), 4(optLevel),
        // 5(variance), 6(durability), 7(quantity)
        String csvContent = String.join("\t",
            "0",                    // 0: unused
            "0",                    // 1: unused
            "PKG_STARTER_BOX",      // 2: packageCodeName
            "ITEM_HP_POTION",       // 3: itemCodeName
            "3",                    // 4: optLevel
            "12345678",             // 5: variance
            "100",                  // 6: durability
            "10"                    // 7: quantity
        );

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("refpackageitem.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16LE));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        when(mockDriver.find(anyString())).thenReturn(Collections.singletonList(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<PackageItemData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        PackageItemData item = items.get(0);
        assertEquals("PKG_STARTER_BOX", item.getPackageCodeName());
        assertEquals("ITEM_HP_POTION", item.getItemCodeName());
        assertEquals(3, item.getOptLevel());
        assertEquals(12345678L, item.getVariance());
        assertEquals(100, item.getDurability());
        assertEquals(10, item.getQuantity());
    }
}
