package org.sokybot.pk2extractor.mediapk2;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.SkillData;

/**
 * Extracts comprehensive skill data from skilldata_*.txt.
 * Pure extraction with streaming callbacks - no caching or persistence.
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
            
            // Field 34: Mastery ID
            if (record.size() > 34 && NumberUtils.isParsable(record.get(34))) {
                builder.masteryId(Integer.parseInt(record.get(34)));
                builder.reqCommonMastery1(Integer.parseInt(record.get(34)));
            }
            
            // Field 36: Req Mastery Level 1
            if (record.size() > 36 && NumberUtils.isParsable(record.get(36))) {
                builder.reqCommonMasteryLevel1(Integer.parseInt(record.get(36)));
            }
            
            // Field 50, 51: Weapons
            if (record.size() > 50 && NumberUtils.isParsable(record.get(50))) {
                builder.reqCastWeapon1(Byte.parseByte(record.get(50)));
            }
            if (record.size() > 51 && NumberUtils.isParsable(record.get(51))) {
                builder.reqCastWeapon2(Byte.parseByte(record.get(51)));
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
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
