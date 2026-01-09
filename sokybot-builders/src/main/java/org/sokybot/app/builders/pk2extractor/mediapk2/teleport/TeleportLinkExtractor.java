package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.TeleportLinkData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts teleport link data from teleportlink.txt files in media.pk2.
 * Teleport links define connections between teleport locations.
 * Reference: RSBot RefTeleportLink
 */
@Slf4j
@Component(service = IExtractor.class)
public class TeleportLinkExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public TeleportLinkExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting teleport link data from media.pk2 file");
        
        List<TeleportLinkData> links = driver.find("(?i)teleportlink.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 5 && !r.get(0).startsWith("//"))
                .map(this::toTeleportLinkData)
                .filter(l -> l.getOwnerTeleportId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} teleport link entries", links.size());
        
        links.forEach(link -> 
            cache.put("TPLINK_" + link.getOwnerTeleportId() + "_" + link.getTargetTeleportId(), link));
    }
    
    private TeleportLinkData toTeleportLinkData(CSVRecord record) {
        var builder = TeleportLinkData.builder();
        
        try {
            // Field 0: Service
            // Field 1: Owner teleport ID
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.ownerTeleportId(Integer.parseInt(field));
            }
            
            // Field 2: Target teleport ID
            field = record.get(2);
            if (NumberUtils.isParsable(field)) {
                builder.targetTeleportId(Integer.parseInt(field));
            }
            
            // Field 3: Fee
            field = record.get(3);
            if (NumberUtils.isParsable(field)) {
                builder.fee(Integer.parseInt(field));
            }
            
            // Field 4: Restrict bind method
            if (record.size() > 4) {
                field = record.get(4);
                if (NumberUtils.isParsable(field)) {
                    builder.restrictBindMethod(Byte.parseByte(field));
                }
            }
            
            // Field 5: Check result
            if (record.size() > 5) {
                field = record.get(5);
                if (NumberUtils.isParsable(field)) {
                    builder.checkResult(Byte.parseByte(field));
                }
            }
            
            // Restriction fields
            if (record.size() > 8) {
                builder.restrict1(parseIntOrZero(record.get(6)));
                builder.data1_1(parseIntOrZero(record.get(7)));
                builder.data1_2(parseIntOrZero(record.get(8)));
            }
            
            if (record.size() > 11) {
                builder.restrict2(parseIntOrZero(record.get(9)));
                builder.data2_1(parseIntOrZero(record.get(10)));
                builder.data2_2(parseIntOrZero(record.get(11)));
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse teleport link record: {}", e.getMessage());
        }
        
        return builder.build();
    }
    
    private int parseIntOrZero(String value) {
        return NumberUtils.isParsable(value) ? Integer.parseInt(value) : 0;
    }
}
