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
import org.sokybot.app.builders.pk2extractor.dto.MagicOptionData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts magic option (blue stat) data from magicoption.txt files in media.pk2.
 * Magic options are item stat bonuses applied through alchemy.
 */
@Slf4j
@Component(service = IExtractor.class)
public class MagicOptionExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public MagicOptionExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting magic option data from media.pk2 file");
        
        List<MagicOptionData> options = driver.find("magicoption.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 10 && !r.get(0).startsWith("//"))
                .map(this::toMagicOptionData)
                .filter(o -> o.getId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} magic option entries", options.size());
        
        // Store in cache for later use
        options.forEach(opt -> cache.put("MAGOPT_" + opt.getId(), opt));
    }
    
    private MagicOptionData toMagicOptionData(CSVRecord record) {
        var builder = MagicOptionData.builder();
        
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
            
            // Field 4: Attribute type
            field = record.get(4);
            if (NumberUtils.isParsable(field)) {
                builder.attributeType(Integer.parseInt(field));
            }
            
            // Field 5-6: Min/Max value
            field = record.get(5);
            if (NumberUtils.isParsable(field)) {
                builder.minValue(Integer.parseInt(field));
            }
            
            field = record.get(6);
            if (NumberUtils.isParsable(field)) {
                builder.maxValue(Integer.parseInt(field));
            }
            
            // Field 7: Required degree
            if (record.size() > 7) {
                field = record.get(7);
                if (NumberUtils.isParsable(field)) {
                    builder.degree(Integer.parseInt(field));
                }
            }
            
            // Field 8-11: Applicable item types
            if (record.size() > 8) {
                field = record.get(8);
                builder.isWeapon(NumberUtils.isParsable(field) && 
                                 BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
            if (record.size() > 9) {
                field = record.get(9);
                builder.isArmor(NumberUtils.isParsable(field) && 
                                BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
            if (record.size() > 10) {
                field = record.get(10);
                builder.isAccessory(NumberUtils.isParsable(field) && 
                                    BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
            if (record.size() > 11) {
                field = record.get(11);
                builder.isShield(NumberUtils.isParsable(field) && 
                                 BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse magic option record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
