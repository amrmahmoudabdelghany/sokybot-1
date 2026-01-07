package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.ShopGroupData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts shop group data from refshopgroup.txt.
 * Reference: RSBot RefShopGroup
 */
@Slf4j
@Component(service = IExtractor.class)
public class ShopGroupExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public ShopGroupExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting shop group data from media.pk2 file");
        
        List<ShopGroupData> groups = driver.find("(?i)refshopgroup.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 4 && !r.get(0).startsWith("//"))
                .map(this::toShopGroupData)
                .filter(g -> g.getId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} shop group entries", groups.size());
        
        groups.forEach(group -> cache.put("SHOP_GROUP_" + group.getId(), group));
    }
    
    private ShopGroupData toShopGroupData(CSVRecord record) {
        var builder = ShopGroupData.builder();
        
        try {
            // Field 1: Country
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.country(Integer.parseInt(field));
            }
            
            // Field 2: ID
            field = record.get(2);
            if (NumberUtils.isParsable(field)) {
                builder.id(Integer.parseInt(field));
            }
            
            // Field 3: CodeName
            builder.codeName(record.get(3));
            
            // Field 4: RefNpcCodeName
            builder.refNpcCodeName(record.get(4));
            
        } catch (Exception e) {
            log.warn("Failed to parse shop group record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
