package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.QuestRewardItemData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts detailed quest item rewards from refquestrewarditem.txt.
 * Reference: RSBot RefQuestRewardItem
 */
@Slf4j
@Component(service = IExtractor.class)
public class QuestRewardItemExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public QuestRewardItemExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting quest item reward data from media.pk2 file");
        
        List<QuestRewardItemData> rewards = driver.find("(?i)refquestrewarditem.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 7 && !r.get(0).startsWith("//"))
                .map(this::toQuestRewardItemData)
                .filter(r -> r.getQuestId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} quest item reward entries", rewards.size());
        
        rewards.forEach(reward -> 
            cache.put("QUEST_ITEM_REWARD_" + reward.getQuestId() + "_" + reward.getItemCodeName(), reward));
    }
    
    private QuestRewardItemData toQuestRewardItemData(CSVRecord record) {
        var builder = QuestRewardItemData.builder();
        
        try {
            // Field 0: Quest ID
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) {
                builder.questId(Integer.parseInt(field));
            }
            
            // Field 1: Quest Code
            builder.questCodeName(record.get(1));
            
            // Field 2: Reward Type
            field = record.get(2);
            if (NumberUtils.isParsable(field)) {
                builder.rewardType(Byte.parseByte(field));
            }
            
            // Field 3: Item Code Name
            builder.itemCodeName(record.get(3));
            
            // Field 4: Optional Item Code
            builder.optionalItemCode(record.get(4));
            
            // Field 5: Optional Count
            field = record.get(5);
            if (NumberUtils.isParsable(field)) {
                builder.optionalItemCount(Integer.parseInt(field));
            }
            
            // Field 6: Achieve Quantity
            field = record.get(6);
            if (NumberUtils.isParsable(field)) {
                builder.achieveQuantity(Integer.parseInt(field));
            }
            
            // Field 7: Rent Item Code (if present)
            if (record.size() > 7) {
                builder.rentItemCodeName(record.get(7));
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse quest item reward record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
