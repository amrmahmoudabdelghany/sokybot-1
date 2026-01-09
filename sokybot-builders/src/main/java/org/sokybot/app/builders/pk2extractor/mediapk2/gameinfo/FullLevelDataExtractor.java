package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.FullLevelData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts comprehensive level data from leveldata.txt.
 * Includes player exp, mastery exp, pet exp, etc.
 * Reference: RSBot RefLevel
 */
@Slf4j
@Component(service = IExtractor.class)
public class FullLevelDataExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public FullLevelDataExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting full level data from media.pk2 file");
        
        List<FullLevelData> levels = driver.find("(?i)leveldata\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 2 && !r.get(0).startsWith("//"))
                .map(this::toFullLevelData)
                .filter(l -> l.getLevel() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} level data entries", levels.size());
        
        levels.forEach(lvl -> cache.put("FULL_LVL_" + lvl.getLevel(), lvl));
    }
    
    private FullLevelData toFullLevelData(CSVRecord record) {
        var builder = FullLevelData.builder();
        
        try {
            // Field 0: Level
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) {
                builder.level(Integer.parseInt(field));
            }
            
            // Field 1: Player Exp (Exp_C)
            field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.playerExp(Long.parseLong(field));
            }
            
            // Field 2: Mastery Exp (Exp_M)
            field = record.get(2);
            if (NumberUtils.isParsable(field)) {
                builder.masteryExp(Integer.parseInt(field));
            }
            
            // Field 9: Pet Exp (Exp_C_Pet2) - Index often 9 in newer files
            if (record.size() > 9) {
                field = record.get(9);
                if (NumberUtils.isParsable(field)) {
                    builder.petExp(Long.parseLong(field));
                }
            }
            
            // Field 10: Pet Stored SP (StoredSp_Pet2)
            if (record.size() > 10) {
                field = record.get(10);
                if (NumberUtils.isParsable(field)) {
                    builder.petStoredSp(Integer.parseInt(field));
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse level data record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
