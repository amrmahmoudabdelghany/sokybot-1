package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.QuestData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts quest data from questdata.txt files in media.pk2.
 * Reference: RSBot QuestLog and QuestType structures.
 */
@Slf4j
@Component(service = IExtractor.class)
public class QuestDataExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public QuestDataExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting quest data from media.pk2 file");
        
        List<QuestData> quests = driver.find("questdata.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 20 && !r.get(0).startsWith("//"))
                .map(this::toQuestData)
                .filter(q -> q.getId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} quest entries", quests.size());
        
        // Store in cache for later use
        quests.forEach(quest -> cache.put("QUEST_" + quest.getId(), quest));
    }
    
    private QuestData toQuestData(CSVRecord record) {
        var builder = QuestData.builder();
        
        try {
            // Field 0: Service/Enabled flag
            // Field 1: ID
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.id(Integer.parseInt(field));
            }
            
            // Field 2: Long ID (codename)
            String longId = record.get(2);
            builder.longId(longId);
            
            // Field 3: Name reference
            field = record.get(3);
            String name = cache.get(field, String.class);
            builder.name(name != null ? name : field);
            
            // Field 4: Quest type
            field = record.get(4);
            if (NumberUtils.isParsable(field)) {
                builder.type(Integer.parseInt(field));
            }
            
            // Field 5-6: Min/Max level
            field = record.get(5);
            if (NumberUtils.isParsable(field)) {
                builder.minLevel(Integer.parseInt(field));
            }
            
            field = record.get(6);
            if (NumberUtils.isParsable(field)) {
                builder.maxLevel(Integer.parseInt(field));
            }
            
            // Field 7: Repeat count
            field = record.get(7);
            if (NumberUtils.isParsable(field)) {
                builder.repeatCount(Integer.parseInt(field));
            }
            
            // Field 8: Party quest flag
            if (record.size() > 8) {
                field = record.get(8);
                if (NumberUtils.isParsable(field)) {
                    builder.isParty(BooleanUtils.toBoolean(Byte.valueOf(field)));
                }
            }
            
            // Field 9: NPC reference
            if (record.size() > 9) {
                field = record.get(9);
                if (NumberUtils.isParsable(field)) {
                    builder.npcRefId(Integer.parseInt(field));
                }
            }
            
            // Reward fields (varies by version)
            if (record.size() > 15) {
                field = record.get(15);
                if (NumberUtils.isParsable(field)) {
                    builder.rewardExp(Integer.parseInt(field));
                }
            }
            
            if (record.size() > 16) {
                field = record.get(16);
                if (NumberUtils.isParsable(field)) {
                    builder.rewardGold(Integer.parseInt(field));
                }
            }
            
            if (record.size() > 17) {
                field = record.get(17);
                if (NumberUtils.isParsable(field)) {
                    builder.rewardSkillPoint(Integer.parseInt(field));
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse quest record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
