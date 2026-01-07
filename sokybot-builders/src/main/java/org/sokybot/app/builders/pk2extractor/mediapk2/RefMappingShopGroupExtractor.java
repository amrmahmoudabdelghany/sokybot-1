package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.RefMappingShopGroupData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts shop to group mappings from refmappingshopgroup.txt.
 * Reference: RSBot RefMappingShopGroup
 */
@Slf4j
@Component(service = IExtractor.class)
public class RefMappingShopGroupExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public RefMappingShopGroupExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting shop group mapping data from media.pk2 file");
        
        List<RefMappingShopGroupData> mappings = driver.find("(?i)refmappingshopgroup.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 3 && !r.get(0).startsWith("//"))
                .map(this::toMappingData)
                .filter(m -> m.getGroupCodeName() != null && !m.getGroupCodeName().isBlank())
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} shop group mapping entries", mappings.size());
        
        mappings.forEach(mapping -> 
            cache.put("MAP_SHOP_GRP_" + mapping.getGroupCodeName() + "_" + mapping.getShopCodeName(), mapping));
    }
    
    private RefMappingShopGroupData toMappingData(CSVRecord record) {
        var builder = RefMappingShopGroupData.builder();
        
        try {
            // Field 1: Country
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.country(Integer.parseInt(field));
            }
            
            // Field 2: Group Code
            builder.groupCodeName(record.get(2));
            
            // Field 3: Shop Code
            builder.shopCodeName(record.get(3));
            
        } catch (Exception e) {
            log.warn("Failed to parse shop group mapping record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
