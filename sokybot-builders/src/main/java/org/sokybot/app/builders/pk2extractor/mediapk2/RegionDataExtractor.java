package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.RegionData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts region/zone data from regioninfo.txt files in media.pk2.
 * Regions define map areas and their properties.
 */
@Slf4j
@Component(service = IExtractor.class)
public class RegionDataExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public RegionDataExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting region data from media.pk2 file");
        
        List<RegionData> regions = driver.find("(?i)regioninfo.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 5 && !r.get(0).startsWith("//"))
                .map(this::toRegionData)
                .filter(r -> r.getId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} region entries", regions.size());
        
        // Store in cache for later use
        regions.forEach(region -> cache.put("REGION_" + region.getId(), region));
    }
    
    private RegionData toRegionData(CSVRecord record) {
        var builder = RegionData.builder();
        
        try {
            // Field 0: Service/Enabled flag
            // Field 1: ID
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.id(Integer.parseInt(field));
            }
            
            // Field 2: Long ID (codename)
            builder.longId(record.get(2));
            
            // Field 3: Name reference
            field = record.get(3);
            String name = cache.get(field, String.class);
            builder.name(name != null ? name : field);
            
            // Field 4: Region type
            field = record.get(4);
            if (NumberUtils.isParsable(field)) {
                builder.type(Integer.parseInt(field));
            }
            
            // Field 5: Area ID
            if (record.size() > 5) {
                field = record.get(5);
                if (NumberUtils.isParsable(field)) {
                    builder.areaId(Integer.parseInt(field));
                }
            }
            
            // Field 6: PvP flag
            if (record.size() > 6) {
                field = record.get(6);
                builder.isPvP(NumberUtils.isParsable(field) && 
                              BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
            // Field 7: Safe zone flag
            if (record.size() > 7) {
                field = record.get(7);
                builder.isSafe(NumberUtils.isParsable(field) && 
                               BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
            // Field 8-9: Min/Max level
            if (record.size() > 8) {
                field = record.get(8);
                if (NumberUtils.isParsable(field)) {
                    builder.minLevel(Integer.parseInt(field));
                }
            }
            
            if (record.size() > 9) {
                field = record.get(9);
                if (NumberUtils.isParsable(field)) {
                    builder.maxLevel(Integer.parseInt(field));
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse region record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
