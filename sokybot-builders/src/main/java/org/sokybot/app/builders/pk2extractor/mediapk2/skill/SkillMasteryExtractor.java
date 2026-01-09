package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.SkillMasteryData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts skill mastery data from refskillmastery.txt.
 * Defines skill trees/groups and their weapon associations.
 * Reference: RSBot RefSkillMastery
 */
@Slf4j
@Component(service = IExtractor.class)
public class SkillMasteryExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public SkillMasteryExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting skill mastery data from media.pk2 file");
        
        List<SkillMasteryData> masteries = driver.find("(?i)refskillmastery.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 5 && !r.get(0).startsWith("//"))
                .map(this::toSkillMasteryData)
                .filter(m -> m.getId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} skill mastery entries", masteries.size());
        
        masteries.forEach(mastery -> 
            cache.put("MASTERY_" + mastery.getId(), mastery));
    }
    
    private SkillMasteryData toSkillMasteryData(CSVRecord record) {
        var builder = SkillMasteryData.builder();
        
        try {
            // Field 0: ID
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) {
                builder.id(Integer.parseInt(field));
            }
            
            // Field 1 or 2/3: Name Code (depends on version, RSBot handles both)
            // Typically index 2 or 3 in vsro files
            if (record.size() > 2) {
                builder.nameCode(record.get(2));
            }
            
            // Field 3: Group Num
            if (record.size() > 3) {
                field = record.get(3);
                if (NumberUtils.isParsable(field)) {
                    builder.groupNum(Byte.parseByte(field));
                }
            }
            
            // Field 4: Description Code
            if (record.size() > 4) {
                builder.descriptionCode(record.get(4));
            }
            
            // Field 6: Tab ID
            if (record.size() > 6) {
                field = record.get(6);
                if (NumberUtils.isParsable(field)) {
                    builder.tabId(Byte.parseByte(field));
                }
            }
            
            // Field 8, 9, 10: Weapon Types
            if (record.size() > 8) {
                field = record.get(8);
                if (NumberUtils.isParsable(field)) {
                    builder.weaponType1(Byte.parseByte(field));
                }
            }
            
            if (record.size() > 9) {
                field = record.get(9);
                if (NumberUtils.isParsable(field)) {
                    builder.weaponType2(Byte.parseByte(field));
                }
            }
            
            if (record.size() > 10) {
                field = record.get(10);
                if (NumberUtils.isParsable(field)) {
                    builder.weaponType3(Byte.parseByte(field));
                }
            }
            
            // Field 11: Icon
            if (record.size() > 11) {
                builder.iconPath(record.get(11));
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse skill mastery record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
