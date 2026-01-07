package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.PackageItemData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts package item data from refpackageitem.txt files in media.pk2.
 * Package items are bundled items (like item mall boxes).
 * Reference: RSBot RefPackageItem
 */
@Slf4j
@Component(service = IExtractor.class)
public class PackageItemExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public PackageItemExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting package item data from media.pk2 file");
        
        List<PackageItemData> packages = driver.find("(?i)refpackageitem.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 5 && !r.get(0).startsWith("//"))
                .map(this::toPackageItemData)
                .filter(p -> p.getPackageCodeName() != null && !p.getPackageCodeName().isBlank())
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} package item entries", packages.size());
        
        packages.forEach(pkg -> 
            cache.put("PKG_" + pkg.getPackageCodeName() + "_" + pkg.getItemCodeName(), pkg));
    }
    
    private PackageItemData toPackageItemData(CSVRecord record) {
        var builder = PackageItemData.builder();
        
        try {
            // Field 0: Service
            // Field 1: Country
            // Field 2: Package code name
            builder.packageCodeName(record.get(2));
            
            // Field 3: Item code name
            builder.itemCodeName(record.get(3));
            
            // Field 4: Opt level
            String field = record.get(4);
            if (NumberUtils.isParsable(field)) {
                builder.optLevel(Byte.parseByte(field));
            }
            
            // Field 5: Variance
            field = record.get(5);
            if (NumberUtils.isParsable(field)) {
                builder.variance(Long.parseLong(field));
            }
            
            // Field 6: Durability/Data
            if (record.size() > 6) {
                field = record.get(6);
                if (NumberUtils.isParsable(field)) {
                    builder.durability(Integer.parseInt(field));
                }
            }
            
            // Field 7: Quantity (if present)
            if (record.size() > 7) {
                field = record.get(7);
                if (NumberUtils.isParsable(field)) {
                    builder.quantity(Integer.parseInt(field));
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse package item record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
