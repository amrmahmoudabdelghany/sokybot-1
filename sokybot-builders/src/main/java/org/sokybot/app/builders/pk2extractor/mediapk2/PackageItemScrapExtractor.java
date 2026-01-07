package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.PackageItemScrapData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts package item scrap data from refscrapofpackageitem.txt.
 * Defines the contents of package items.
 * Reference: RSBot RefPackageItemScrap
 */
@Slf4j
@Component(service = IExtractor.class)
public class PackageItemScrapExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public PackageItemScrapExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting package item scrap data from media.pk2 file");
        
        List<PackageItemScrapData> scraps = driver.find("(?i)refscrapofpackageitem.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 4 && !r.get(0).startsWith("//"))
                .map(this::toPackageItemScrapData)
                .filter(s -> s.getPackageItemCodeName() != null && !s.getPackageItemCodeName().isBlank())
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} package item scrap entries", scraps.size());
        
        // Cache could use composite key or list if multiple scraps per package
        // Here we just cache individual entries
        scraps.forEach(scrap -> 
            cache.put("PKGSCRAP_" + scrap.getPackageItemCodeName() + "_" + scrap.getIndex(), scrap));
    }
    
    private PackageItemScrapData toPackageItemScrapData(CSVRecord record) {
        var builder = PackageItemScrapData.builder();
        
        try {
            // Field 2: RefPackageItemCodeName
            builder.packageItemCodeName(record.get(2));
            
            // Field 3: RefItemCodeName
            builder.itemCodeName(record.get(3));
            
            // Field 4: OptLevel
            String field = record.get(4);
            if (NumberUtils.isParsable(field)) {
                builder.optLevel(Byte.parseByte(field));
            }
            
            // Field 5: Variance
            field = record.get(5);
            if (NumberUtils.isParsable(field)) {
                builder.variance(Long.parseLong(field));
            }
            
            // Field 6: Data
            field = record.get(6);
            if (NumberUtils.isParsable(field)) {
                builder.data(Integer.parseInt(field));
            }
            
            // Field: Index (last column typically)
            // Checking if last column is index
            if (record.size() > 0) {
                 field = record.get(record.size() - 1); // Often index is last
                 if (NumberUtils.isParsable(field)) {
                     builder.index(Integer.parseInt(field));
                 }
            }

        } catch (Exception e) {
            log.warn("Failed to parse package scrap record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
