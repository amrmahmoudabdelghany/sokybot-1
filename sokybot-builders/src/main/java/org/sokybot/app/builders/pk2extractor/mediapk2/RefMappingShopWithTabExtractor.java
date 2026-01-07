package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.RefMappingShopWithTabData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts shop to tab mappings from refmappingshopwithtab.txt.
 * Reference: RSBot RefMappingShopWithTab
 */
@Slf4j
@Component(service = IExtractor.class)
public class RefMappingShopWithTabExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public RefMappingShopWithTabExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting shop tab mapping data from media.pk2 file");
        
        List<RefMappingShopWithTabData> mappings = driver.find("(?i)refmappingshopwithtab.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 3 && !r.get(0).startsWith("//"))
                .map(this::toMappingData)
                .filter(m -> m.getShopCodeName() != null && !m.getShopCodeName().isBlank())
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} shop tab mapping entries", mappings.size());
        
        mappings.forEach(mapping -> 
            cache.put("MAP_SHOP_TAB_" + mapping.getShopCodeName() + "_" + mapping.getTabCodeName(), mapping));
    }
    
    private RefMappingShopWithTabData toMappingData(CSVRecord record) {
        var builder = RefMappingShopWithTabData.builder();
        
        try {
            // Field 1: Country
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.country(Integer.parseInt(field));
            }
            
            // Field 2: Shop Code
            builder.shopCodeName(record.get(2));
            
            // Field 3: Tab Code
            builder.tabCodeName(record.get(3));
            
        } catch (Exception e) {
            log.warn("Failed to parse shop tab mapping record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
