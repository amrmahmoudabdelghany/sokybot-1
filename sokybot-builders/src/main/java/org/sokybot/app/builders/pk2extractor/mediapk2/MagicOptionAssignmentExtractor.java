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
import org.sokybot.app.builders.pk2extractor.dto.MagicOptionAssignmentData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts magic option assignment data from refmagicoptassign.txt (or magicoption_assign.txt).
 * Defines allowed magic options for specific item types.
 * Reference: RSBot RefMagicOptAssign
 */
@Slf4j
@Component(service = IExtractor.class)
public class MagicOptionAssignmentExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public MagicOptionAssignmentExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting magic option assignment data from media.pk2 file");
        
        // Regex to match likely filenames
        List<MagicOptionAssignmentData> assignments = driver.find("(?i)(ref)?magicopt.*assign.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 4 && !r.get(0).startsWith("//"))
                .map(this::toMagicOptionAssignmentData)
                .filter(a -> a.getRace() != 0 || a.getTypeId3() != 0) // Basic valid check
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} magic option assignment entries", assignments.size());
        
        assignments.forEach(assign -> 
            cache.put("MAGOPTASSIGN_" + assign.getRace() + "_" + assign.getTypeId3() + "_" + assign.getTypeId4(), assign));
    }
    
    private MagicOptionAssignmentData toMagicOptionAssignmentData(CSVRecord record) {
        var builder = MagicOptionAssignmentData.builder();
        
        try {
            // Field 0: Service
            // Field 1: Race
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.race(Byte.parseByte(field));
            }
            
            // Field 2: TypeID3
            field = record.get(2);
            if (NumberUtils.isParsable(field)) {
                builder.typeId3(Byte.parseByte(field));
            }
            
            // Field 3: TypeID4
            field = record.get(3);
            if (NumberUtils.isParsable(field)) {
                builder.typeId4(Byte.parseByte(field));
            }
            
            // Field 4+: Available Magic Option Codes
            List<String> options = new ArrayList<>();
            for (int i = 4; i < record.size(); i++) {
                String opt = record.get(i);
                if (opt != null && !opt.isBlank() && !opt.equals("xxx") && !opt.equals("NULL")) {
                    options.add(opt);
                }
            }
            builder.availableMagicOptions(options);
            
        } catch (Exception e) {
            log.warn("Failed to parse magic option assignment record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
