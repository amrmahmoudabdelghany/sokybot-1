package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.ExtraItemOptAbilityData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts extra item opt level ability data from refextraabilitybyequipitemoptlevel.txt.
 * Reference: RSBot RefExtraAbilityByEquipItemOptLevel
 */
@Slf4j
@Component(service = IExtractor.class)
public class ExtraItemOptAbilityExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public ExtraItemOptAbilityExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting extra opt ability data from media.pk2 file");
        
        List<ExtraItemOptAbilityData> abilities = driver.find("(?i)refextraabilitybyequipitemoptlevel.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 5 && !r.get(0).startsWith("//"))
                .map(this::toExtraItemOptAbilityData)
                .filter(a -> a.getItemId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} extra opt ability entries", abilities.size());
        
        abilities.forEach(ability -> 
            cache.put("EXTRA_OPTLVL_" + ability.getItemId() + "_" + ability.getOptLevel(), ability));
    }
    
    private ExtraItemOptAbilityData toExtraItemOptAbilityData(CSVRecord record) {
        var builder = ExtraItemOptAbilityData.builder();
        
        try {
            // Field 1: Item ID
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.itemId(Integer.parseInt(field));
            }
            
            // Field 2: Opt Level
            field = record.get(2);
            if (NumberUtils.isParsable(field)) {
                builder.optLevel(Byte.parseByte(field));
            }
            
            // Fields 20+: Skills (Fixed count of 5 in RSBot, varies in file)
            // Typically starts around index 3 or 4 in actual file
            List<Integer> skills = new ArrayList<>();
            // Assuming skills start from index 3 or later based on file structure
            for (int i = 3; i < record.size(); i++) {
                field = record.get(i);
                if (NumberUtils.isParsable(field)) {
                    int skillId = Integer.parseInt(field);
                    if (skillId > 0) skills.add(skillId);
                }
            }
            builder.skillIds(skills);
            
        } catch (Exception e) {
            log.warn("Failed to parse extra opt ability record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
