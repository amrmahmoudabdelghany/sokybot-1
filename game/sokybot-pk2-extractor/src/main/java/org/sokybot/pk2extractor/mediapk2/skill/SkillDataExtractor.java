package org.sokybot.pk2extractor.mediapk2.skill;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.skill.SkillData;
import org.sokybot.pk2extractor.dto.skill.SkillEffect;
import org.sokybot.pk2extractor.dto.skill.SkillTargetType;

/**
 * Extracts comprehensive skill data from skilldata_*.txt.
 * Enhanced to parse skill parameters/effects based on skrillax.
 */
public class SkillDataExtractor implements IExtractor<SkillData> {

    private static final String NAME = "Skill Data";
    private static final String FILE_PATTERN = "(?i)skilldata_(\\d+)(enc)?.txt$";

    @Override
    public Class<SkillData> getDtoClass() {
        return SkillData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<SkillData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            driver.find(FILE_PATTERN).stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 70 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    SkillData dto = toSkillData(record);
                    if (dto != null && dto.getRefId() > 0) {
                        counter[0]++;
                        
                        if (listener != null) {
                            listener.onExtracted(dto);
                        }
                        
                        if (progressListener != null) {
                            progressListener.onProgress(NAME, counter[0], -1, 
                                String.valueOf(dto.getRefId()));
                        }
                    }
                });
            
            long duration = System.currentTimeMillis() - startTime;
            if (listener != null) {
                listener.onComplete(counter[0]);
            }
            if (progressListener != null) {
                progressListener.onComplete(NAME, counter[0], duration);
            }
            
        } catch (Exception e) {
            if (listener != null) {
                listener.onError(e);
            }
            if (progressListener != null) {
                progressListener.onError(NAME, e);
            }
        }
    }

    private SkillData toSkillData(CSVRecord record) {
        try {
            var builder = SkillData.builder();
            
            // Field 1: RefID
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.refId(Integer.parseInt(field));
            }
            
            // Field 3: LongID
            builder.longId(record.get(3));
            
            // Field 11: PreparingTime
            if (record.size() > 11 && NumberUtils.isParsable(record.get(11))) {
                builder.preparingTime(Integer.parseInt(record.get(11)));
            }
            
            // Field 12: CastingTime
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
            
            // Field 23: Target Type
            if (record.size() > 23 && NumberUtils.isParsable(record.get(23))) {
                int targetCode = Integer.parseInt(record.get(23));
                builder.targetType(SkillTargetType.fromCode(targetCode));
            }
            
            // Field 26: Range
            if (record.size() > 26 && NumberUtils.isParsable(record.get(26))) {
                builder.range(Integer.parseInt(record.get(26)));
            }
            
            // Field 27: Attack Distance
            if (record.size() > 27 && NumberUtils.isParsable(record.get(27))) {
                builder.attackDistance(Integer.parseInt(record.get(27)));
            }
            
            // Field 28: AOE Range
            if (record.size() > 28 && NumberUtils.isParsable(record.get(28))) {
                builder.aoeRange(Integer.parseInt(record.get(28)));
            }
            
            // Field 29: Max Targets
            if (record.size() > 29 && NumberUtils.isParsable(record.get(29))) {
                builder.maxTargets(Integer.parseInt(record.get(29)));
            }
            
            // Field 34: Mastery ID
            if (record.size() > 34 && NumberUtils.isParsable(record.get(34))) {
                builder.masteryId(Integer.parseInt(record.get(34)));
                builder.reqCommonMastery1(Integer.parseInt(record.get(34)));
            }
            
            // Field 36: Req Mastery Level 1
            if (record.size() > 36 && NumberUtils.isParsable(record.get(36))) {
                builder.reqCommonMasteryLevel1(Integer.parseInt(record.get(36)));
            }
            
            // Field 37: Skill Level
            if (record.size() > 37 && NumberUtils.isParsable(record.get(37))) {
                builder.skillLevel(Integer.parseInt(record.get(37)));
            }
            
            // Field 50, 51: Weapons
            if (record.size() > 50 && NumberUtils.isParsable(record.get(50))) {
                builder.reqCastWeapon1(Byte.parseByte(record.get(50)));
            }
            if (record.size() > 51 && NumberUtils.isParsable(record.get(51))) {
                builder.reqCastWeapon2(Byte.parseByte(record.get(51)));
            }
            
            // Weapon requirements array (columns 50-55)
            if (record.size() > 55) {
                int[] weapons = new int[6];
                int count = 0;
                for (int i = 50; i <= 55; i++) {
                    if (NumberUtils.isParsable(record.get(i))) {
                        int w = Integer.parseInt(record.get(i));
                        if (w > 0) weapons[count++] = w;
                    }
                }
                if (count > 0) {
                    int[] trimmed = new int[count];
                    System.arraycopy(weapons, 0, trimmed, 0, count);
                    builder.weaponRequirements(trimmed);
                }
            }
            
            // Field 52: Consume HP
            if (record.size() > 52 && NumberUtils.isParsable(record.get(52))) {
                builder.consumeHP(Integer.parseInt(record.get(52)));
            }
            
            // Field 53: Consume MP
            if (record.size() > 53 && NumberUtils.isParsable(record.get(53))) {
                builder.MP(Integer.parseInt(record.get(53)));
            }
            
            // Field 61: Icon
            if (record.size() > 61) {
                builder.iconPath(record.get(61));
            }
            
            // Field 62: Name
            if (record.size() > 62) {
                builder.name(record.get(62));
            }
            
            // Skill Effects/Parameters (columns 69-118, grouped as paramType/value pairs)
            // Layout: paramType1, value1, value2_1, prob1, duration1, paramType2, ...
            List<SkillEffect> effects = parseSkillEffects(record);
            if (!effects.isEmpty()) {
                builder.effects(effects);
            }
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Parse skill effects from parameter columns.
     * Effects are stored as groups of 5 values: paramType, value, value2, probability, duration
     */
    private List<SkillEffect> parseSkillEffects(CSVRecord record) {
        List<SkillEffect> effects = new ArrayList<>();
        
        // Parse up to 10 effect slots (columns 69-118, 5 values each)
        int startCol = 69;
        for (int slot = 0; slot < 10; slot++) {
            int col = startCol + (slot * 5);
            if (record.size() <= col + 4) break;
            
            String paramTypeStr = record.get(col);
            if (!NumberUtils.isParsable(paramTypeStr)) continue;
            
            int paramType = Integer.parseInt(paramTypeStr);
            if (paramType == 0) continue; // Skip empty slots
            
            int value = safeParseInt(record.get(col + 1));
            int value2 = safeParseInt(record.get(col + 2));
            int probability = safeParseInt(record.get(col + 3));
            int duration = safeParseInt(record.get(col + 4));
            
            effects.add(SkillEffect.builder()
                .paramType(paramType)
                .value(value)
                .value2(value2)
                .probability(probability)
                .duration(duration)
                .build());
        }
        
        return effects;
    }
    
    private int safeParseInt(String str) {
        return NumberUtils.isParsable(str) ? Integer.parseInt(str) : 0;
    }
}
