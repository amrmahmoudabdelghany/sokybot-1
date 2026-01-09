package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.SkillByItemOptLevelData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts skill link data from refskillbyitemoptlevel.txt.
 * Reference: RSBot RefSkillByItemOptLevel
 */
@Slf4j
@Component(service = IExtractor.class)
public class SkillByItemOptLevelExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public SkillByItemOptLevelExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting skill-opt link data from media.pk2 file");
        
        List<SkillByItemOptLevelData> links = driver.find("(?i)refskillbyitemoptlevel.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 1 && !r.get(0).startsWith("//"))
                .map(this::toSkillByItemOptLevelData)
                .filter(l -> l.getLinkId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} skill-opt link entries", links.size());
        
        links.forEach(link -> cache.put("SKILL_OPTLINK_" + link.getLinkId(), link));
    }
    
    private SkillByItemOptLevelData toSkillByItemOptLevelData(CSVRecord record) {
        var builder = SkillByItemOptLevelData.builder();
        
        try {
            // Field 0: Link ID
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) {
                builder.linkId(Integer.parseInt(field));
            }
            
            // Field 1: Skill ID
            field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.skillId(Integer.parseInt(field));
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse skill-opt link record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
