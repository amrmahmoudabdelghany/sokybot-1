package org.sokybot.pk2extractor.mediapk2;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.SkillMasteryData;

/**
 * Extracts skill mastery data from refskillmastery.txt.
 * Pure extraction with streaming callbacks.
 */
public class SkillMasteryExtractor implements IExtractor<SkillMasteryData> {

    private static final String NAME = "Skill Mastery Data";
    private static final String FILE_PATTERN = "(?i)refskillmastery.*\\.txt$";

    @Override
    public Class<SkillMasteryData> getDtoClass() {
        return SkillMasteryData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<SkillMasteryData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            driver.find(FILE_PATTERN).stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 5 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    SkillMasteryData dto = toSkillMasteryData(record);
                    if (dto != null && dto.getId() > 0) {
                        counter[0]++;
                        
                        if (listener != null) {
                            listener.onExtracted(dto);
                        }
                        
                        if (progressListener != null) {
                            progressListener.onProgress(NAME, counter[0], -1, 
                                String.valueOf(dto.getId()));
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

    private SkillMasteryData toSkillMasteryData(CSVRecord record) {
        try {
            var builder = SkillMasteryData.builder();
            
            // Field 0: ID
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) {
                builder.id(Integer.parseInt(field));
            }
            
            // Field 2: Name Code
            if (record.size() > 2) {
                builder.nameCode(record.get(2));
            }
            
            // Field 3: Group Num
            if (record.size() > 3) {
                field = record.get(3);
                if (NumberUtils.isParsable(field)) {
                    builder.groupNum(Byte.parseByte(field));
                }
            }
            
            // Field 4: Description Code
            if (record.size() > 4) {
                builder.descriptionCode(record.get(4));
            }
            
            // Field 6: Tab ID
            if (record.size() > 6) {
                field = record.get(6);
                if (NumberUtils.isParsable(field)) {
                    builder.tabId(Byte.parseByte(field));
                }
            }
            
            // Field 8, 9, 10: Weapon Types
            if (record.size() > 8) {
                field = record.get(8);
                if (NumberUtils.isParsable(field)) {
                    builder.weaponType1(Byte.parseByte(field));
                }
            }
            
            if (record.size() > 9) {
                field = record.get(9);
                if (NumberUtils.isParsable(field)) {
                    builder.weaponType2(Byte.parseByte(field));
                }
            }
            
            if (record.size() > 10) {
                field = record.get(10);
                if (NumberUtils.isParsable(field)) {
                    builder.weaponType3(Byte.parseByte(field));
                }
            }
            
            // Field 11: Icon
            if (record.size() > 11) {
                builder.iconPath(record.get(11));
            }
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
