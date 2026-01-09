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
import org.sokybot.app.builders.pk2extractor.dto.QuestRewardData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts detailed quest reward data from refquestreward.txt (or similar).
 * Reference: RSBot RefQuestReward
 */
@Slf4j
@Component(service = IExtractor.class)
public class QuestRewardExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public QuestRewardExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting quest reward data from media.pk2 file");
        
        List<QuestRewardData> rewards = driver.find("(?i)refquestreward.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 5 && !r.get(0).startsWith("//"))
                .map(this::toQuestRewardData)
                .filter(r -> r.getQuestId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} quest reward entries", rewards.size());
        
        rewards.forEach(reward -> 
            cache.put("QUEST_REWARD_" + reward.getQuestId(), reward));
    }
    
    private QuestRewardData toQuestRewardData(CSVRecord record) {
        var builder = QuestRewardData.builder();
        
        try {
            // Field 0: Quest ID
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) {
                builder.questId(Integer.parseInt(field));
            }
            
            // Field 1: Quest Code Name
            builder.questCodeName(record.get(1));
            
            // Field 2: IsView
            field = record.get(2);
            if (NumberUtils.isParsable(field)) {
                builder.isView(BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
            // Field 4: IsItemReward
            if (record.size() > 4) {
                 field = record.get(4);
                 if (NumberUtils.isParsable(field)) {
                     builder.isItemReward(BooleanUtils.toBoolean(Byte.valueOf(field)));
                 }
            }
            
            // Field 10-18: Rewards
            if (record.size() > 10) {
                if (NumberUtils.isParsable(record.get(10))) builder.gold(Integer.parseInt(record.get(10)));
                if (NumberUtils.isParsable(record.get(11))) builder.exp(Integer.parseInt(record.get(11)));
                if (NumberUtils.isParsable(record.get(12))) builder.spExp(Integer.parseInt(record.get(12)));
                if (NumberUtils.isParsable(record.get(13))) builder.sp(Integer.parseInt(record.get(13)));
                if (NumberUtils.isParsable(record.get(14))) builder.ap(Integer.parseInt(record.get(14)));
                
                builder.apType(record.get(15));
                
                if (NumberUtils.isParsable(record.get(16))) builder.hwan(Byte.parseByte(record.get(16)));
                if (NumberUtils.isParsable(record.get(17))) builder.inventorySlots(Byte.parseByte(record.get(17)));
                if (NumberUtils.isParsable(record.get(18))) builder.itemRewardType(Byte.parseByte(record.get(18)));
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse quest reward record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
