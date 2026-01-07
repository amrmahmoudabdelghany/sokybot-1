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
import org.sokybot.app.builders.pk2extractor.dto.SkillData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts comprehensive skill data from skilldata_*.txt.
 * Uses enhanced SkillData DTO with more fields than legacy SkillEntityExtractor.
 */
@Slf4j
@Component(service = IExtractor.class)
public class SkillDataExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public SkillDataExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting comprehensive skill data (SkillData) from media.pk2");
        
        List<SkillData> skills = driver.find("(?i)skilldata_(\\d+)(enc)?.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 70 && !r.get(0).startsWith("//"))
                .map(this::toSkillData)
                .filter(s -> s.getRefId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} detailed skill entries", skills.size());
        
        skills.forEach(skill -> cache.put("SKILL_DATA_" + skill.getRefId(), skill));
    }
    
    private SkillData toSkillData(CSVRecord record) {
        var builder = SkillData.builder();
        
        try {
            // Field 1: RefID
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.refId(Integer.parseInt(field));
            }
            
            // Field 3: LongID
            builder.longId(record.get(3));
            
            // Field 11: Action_PreparingTime
            if (record.size() > 11 && NumberUtils.isParsable(record.get(11))) {
                builder.preparingTime(Integer.parseInt(record.get(11)));
            }
            
            // Field 12 or 13: Action_CastingTime (Often 12 in structure, but code says 13. Let's trust older extractor index 13 if it worked, or check RefSkill logic)
            // RefSkill (Step 728) says:
            // 11: PreparingTime
            // 12: CastingTime
            // 13: ActionDuration
            // 14: ReuseDelay
            // The old extractor used 13 for castTime and 14 for cooldown.
            // Let's stick with RefSkill indices which seem standard.
            
            if (record.size() > 12 && NumberUtils.isParsable(record.get(12))) {
                builder.castTime(Integer.parseInt(record.get(12)));
            }
            
            // Field 13: Duration
            if (record.size() > 13 && NumberUtils.isParsable(record.get(13))) {
                builder.duration(Integer.parseInt(record.get(13)));
            }
            
            // Field 14: Cooldown
            if (record.size() > 14 && NumberUtils.isParsable(record.get(14))) {
                builder.cooldown(Integer.parseInt(record.get(14)));
            }
            
            // Field 22: Target Required
            if (record.size() > 22 && NumberUtils.isParsable(record.get(22))) {
                builder.targetRequired(BooleanUtils.toBoolean(Byte.valueOf(record.get(22))));
            }
            
            // Field 34: Mastery 1 / Mastery ID
            if (record.size() > 34 && NumberUtils.isParsable(record.get(34))) {
                builder.masteryId(Integer.parseInt(record.get(34)));
                builder.reqCommonMastery1(Integer.parseInt(record.get(34)));
            }
            
            // Field 36: Req Mastery Level 1
            if (record.size() > 36 && NumberUtils.isParsable(record.get(36))) {
                builder.reqCommonMasteryLevel1(Integer.parseInt(record.get(36)));
            }
            
            // Field 50, 51: Weapons (RefSkill says 50/51)
            // Old extractor didn't use this.
            if (record.size() > 50 && NumberUtils.isParsable(record.get(50))) {
                builder.reqCastWeapon1(Byte.parseByte(record.get(50)));
            }
            
            if (record.size() > 51 && NumberUtils.isParsable(record.get(51))) {
                builder.reqCastWeapon2(Byte.parseByte(record.get(51)));
            }
            
            // Field 53: Consume MP
            if (record.size() > 53 && NumberUtils.isParsable(record.get(53))) {
                builder.MP(Integer.parseInt(record.get(53)));
            }
            
            // Field 54: Consume HP (RefSkill: 52 is HP, 53 is MP. Wait. RefSkill says 52 HP, 53 MP. Old extractor says 53 is MP. Safe assumption.)
            // Let's try 52 for HP
            if (record.size() > 52 && NumberUtils.isParsable(record.get(52))) {
                builder.consumeHP(Integer.parseInt(record.get(52)));
            }
            
            // Field 61: Icon (RefSkill 61)
            if (record.size() > 61) {
                builder.iconPath(record.get(61));
            }
            
            // Field 62: Name (RefSkill 62)
            builder.name(record.get(62)); // Use SN key directly or localize if cache is available
            // If cache available, localize:
            // String name = cache.get(record.get(62));
            // if(name != null) builder.name(name);

        } catch (Exception e) {
            log.warn("Failed to parse detailed skill record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
