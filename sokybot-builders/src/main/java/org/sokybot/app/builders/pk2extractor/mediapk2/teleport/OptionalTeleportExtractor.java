package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.OptionalTeleportData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts optional teleport data from refoptionalteleport.txt.
 * Defines special/dynamic teleport points.
 * Reference: RSBot RefOptionalTeleport
 */
@Slf4j
@Component(service = IExtractor.class)
public class OptionalTeleportExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public OptionalTeleportExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting optional teleport data from media.pk2 file");
        
        List<OptionalTeleportData> teleports = driver.find("(?i)refoptionalteleport.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 4 && !r.get(0).startsWith("//"))
                .map(this::toOptionalTeleportData)
                .filter(t -> t.getId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} optional teleport entries", teleports.size());
        
        teleports.forEach(t -> cache.put("OPT_TELE_" + t.getId(), t));
    }
    
    private OptionalTeleportData toOptionalTeleportData(CSVRecord record) {
        var builder = OptionalTeleportData.builder();
        
        try {
            // Field 1: ID
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.id(Integer.parseInt(field));
            }
            
            // Field 2: ObjName128
            builder.objName128(record.get(2));
            
            // Field 3: ZoneName128
            builder.zoneName128(record.get(3));
            
            // Field 4: RegionID
            field = record.get(4);
            if (NumberUtils.isParsable(field)) {
                builder.regionId(Integer.parseInt(field));
            }
            
            // Fields 5, 6, 7: Pos X, Z, Y
            if (record.size() > 7) {
                if (NumberUtils.isParsable(record.get(5))) builder.posX(Float.parseFloat(record.get(5)));
                if (NumberUtils.isParsable(record.get(6))) builder.posZ(Float.parseFloat(record.get(6)));
                if (NumberUtils.isParsable(record.get(7))) builder.posY(Float.parseFloat(record.get(7)));
            }
            
            // Field 8: WorldID
            if (record.size() > 8) {
                field = record.get(8);
                if (NumberUtils.isParsable(field)) {
                    builder.worldId(Integer.parseInt(field));
                }
            }
            
            // Field 11, 12: Min/Max Level (Indices vary, typically around 11/12)
            if (record.size() > 12) {
                 if (NumberUtils.isParsable(record.get(11))) builder.minLevel(Integer.parseInt(record.get(11)));
                 if (NumberUtils.isParsable(record.get(12))) builder.maxLevel(Integer.parseInt(record.get(12)));
            }

        } catch (Exception e) {
            log.warn("Failed to parse optional teleport record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
