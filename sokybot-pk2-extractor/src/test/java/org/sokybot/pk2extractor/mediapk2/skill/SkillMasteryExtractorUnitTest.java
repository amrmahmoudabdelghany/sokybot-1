package org.sokybot.pk2extractor.mediapk2.skill;

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
import org.sokybot.pk2extractor.dto.skill.SkillMasteryData;
import org.sokybot.pk2extractor.test.AbstractExtractorUnitTest;

class SkillMasteryExtractorUnitTest extends AbstractExtractorUnitTest<SkillMasteryData> {

    @Override
    protected IExtractor<SkillMasteryData> createExtractor() {
        return new SkillMasteryExtractor();
    }

    @Test
    void testExtractSkillMastery() throws Exception {
        // Construct a sample CSV line with Tab delimiter
        // Columns (based on SkillMasteryExtractor):
        // 0: id, 1: ?, 2: nameCode, 3: groupNum, 4: descriptionCode
        // 5: ?, 6: tabId, 7: ?, 8: weaponType1, 9: weaponType2, 10: weaponType3, 11: iconPath
        String csvContent = String.join("\t",
            "501",              // 0: id
            "0",                // 1: (unused)
            "SN_MASTERY_SWORD", // 2: nameCode
            "1",                // 3: groupNum
            "SD_MASTERY_SWORD", // 4: descriptionCode
            "0",                // 5: (unused)
            "2",                // 6: tabId
            "0",                // 7: (unused)
            "1",                // 8: weaponType1
            "2",                // 9: weaponType2
            "3",                // 10: weaponType3
            "icon/skill/mastery_sword.ddj" // 11: iconPath
        );

        JMXFile mockFile = mock(JMXFile.class);
        when(mockFile.getName()).thenReturn("refskillmastery.txt");
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_16LE));
        when(mockFile.getInputStream()).thenReturn(inputStream);

        when(mockDriver.find(anyString())).thenReturn(Collections.singletonList(mockFile));

        extractor.extract(mockDriver, testListener, testProgressListener);

        List<SkillMasteryData> items = testListener.getExtractedItems();
        assertEquals(1, items.size());
        
        SkillMasteryData item = items.get(0);
        assertEquals(501, item.getId());
        assertEquals("SN_MASTERY_SWORD", item.getNameCode());
        assertEquals(1, item.getGroupNum());
        assertEquals("SD_MASTERY_SWORD", item.getDescriptionCode());
        assertEquals(2, item.getTabId());
        assertEquals(1, item.getWeaponType1());
        assertEquals(2, item.getWeaponType2());
        assertEquals(3, item.getWeaponType3());
        assertEquals("icon/skill/mastery_sword.ddj", item.getIconPath());
    }
}
