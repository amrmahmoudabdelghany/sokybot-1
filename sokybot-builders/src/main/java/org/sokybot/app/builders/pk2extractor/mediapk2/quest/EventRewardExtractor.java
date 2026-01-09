package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.EventRewardData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts event reward item data from refeventrewarditems.txt.
 * Defines items given as rewards during events.
 * Reference: RSBot RefEventRewardItems
 */
@Slf4j
@Component(service = IExtractor.class)
public class EventRewardExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public EventRewardExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting event reward data from media.pk2 file");
        
        List<EventRewardData> rewards = driver.find("(?i)refeventrewarditems.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 3 && !r.get(0).startsWith("//"))
                .map(this::toEventRewardData)
                .filter(r -> r.getEventId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} event reward entries", rewards.size());
        
        rewards.forEach(reward -> 
            cache.put("EVTREW_" + reward.getEventId() + "_" + reward.getItemCodeName(), reward));
    }
    
    private EventRewardData toEventRewardData(CSVRecord record) {
        var builder = EventRewardData.builder();
        
        try {
            // Field 0: Event ID
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) {
                builder.eventId(Integer.parseInt(field));
            }
            
            // Field 1: Event Code Name
            builder.eventCodeName(record.get(1));
            
            // Field 2: Item Code Name
            builder.itemCodeName(record.get(2));
            
            // Field 3: Amount
            field = record.get(3);
            if (NumberUtils.isParsable(field)) {
                builder.itemAmount(Integer.parseInt(field));
            }
            
            // Field 7: Min Level (from RSBot reference, index might vary)
            if (record.size() > 7) {
                field = record.get(7);
                if (NumberUtils.isParsable(field)) {
                    builder.minRequiredLevel(Integer.parseInt(field));
                }
            }
            
            // Field 8: Max Level
            if (record.size() > 8) {
                field = record.get(8);
                if (NumberUtils.isParsable(field)) {
                    builder.maxRequiredLevel(Integer.parseInt(field));
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse event reward record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
