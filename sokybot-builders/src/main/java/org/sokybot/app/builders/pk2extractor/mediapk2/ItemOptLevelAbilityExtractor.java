package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.ItemOptLevelAbilityData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts item optimization level ability data from refabilitybyitemoptlevel.txt.
 * Defines stat bonuses granted at each enhancement level.
 * Reference: RSBot RefAbilityByItemOptLevel
 */
@Slf4j
@Component(service = IExtractor.class)
public class ItemOptLevelAbilityExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public ItemOptLevelAbilityExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting item opt level ability data from media.pk2 file");
        
        List<ItemOptLevelAbilityData> abilities = driver.find("(?i)refabilitybyitemoptlevel.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 3 && !r.get(0).startsWith("//"))
                .map(this::toItemOptLevelAbilityData)
                .filter(a -> a.getId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} item opt level ability entries", abilities.size());
        
        abilities.forEach(ability -> 
            cache.put("OPTLVL_" + ability.getItemId() + "_" + ability.getOptLevel(), ability));
    }
    
    private ItemOptLevelAbilityData toItemOptLevelAbilityData(CSVRecord record) {
        var builder = ItemOptLevelAbilityData.builder();
        
        try {
            // Field 0: Service
            // Field 1: ID
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.id(Integer.parseInt(field));
            }
            
            // Field 2: Item ID
            field = record.get(2);
            if (NumberUtils.isParsable(field)) {
                builder.itemId(Integer.parseInt(field));
            }
            
            // Field 3: Opt level
            field = record.get(3);
            if (NumberUtils.isParsable(field)) {
                builder.optLevel(Byte.parseByte(field));
            }
            
            // Field 4: Skill/ability ID (if present)
            if (record.size() > 4) {
                field = record.get(4);
                if (NumberUtils.isParsable(field)) {
                    builder.skillId(Integer.parseInt(field));
                }
            }
            
            // Field 5: Bonus value (if present)
            if (record.size() > 5) {
                field = record.get(5);
                if (NumberUtils.isParsable(field)) {
                    builder.bonusValue(Integer.parseInt(field));
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse opt level ability record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
