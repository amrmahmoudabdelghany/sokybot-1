package org.sokybot.pk2extractor.mediapk2.gameinfo;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.progression.FullLevelData;

/**
 * Extracts comprehensive level data from leveldata.txt.
 * Pure extraction with streaming callbacks.
 */
public class FullLevelDataExtractor implements IExtractor<FullLevelData> {

    private static final String NAME = "Full Level Data";
    private static final String FILE_PATTERN = "(?i)leveldata\\.txt$";

    @Override
    public Class<FullLevelData> getDtoClass() {
        return FullLevelData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<FullLevelData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            driver.find(FILE_PATTERN).stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 2 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    FullLevelData dto = toFullLevelData(record);
                    if (dto != null && dto.getLevel() > 0) {
                        counter[0]++;
                        
                        if (listener != null) {
                            listener.onExtracted(dto);
                        }
                        
                        if (progressListener != null) {
                            progressListener.onProgress(NAME, counter[0], -1, 
                                String.valueOf(dto.getLevel()));
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

    private FullLevelData toFullLevelData(CSVRecord record) {
        try {
            var builder = FullLevelData.builder();
            
            // Field 0: Level
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) {
                builder.level(Integer.parseInt(field));
            }
            
            // Field 1: Player Exp
            field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.playerExp(Long.parseLong(field));
            }
            
            // Field 2: Mastery Exp
            field = record.get(2);
            if (NumberUtils.isParsable(field)) {
                builder.masteryExp(Integer.parseInt(field));
            }
            
            // Field 9: Pet Exp
            if (record.size() > 9) {
                field = record.get(9);
                if (NumberUtils.isParsable(field)) {
                    builder.petExp(Long.parseLong(field));
                }
            }
            
            // Field 10: Pet Stored SP
            if (record.size() > 10) {
                field = record.get(10);
                if (NumberUtils.isParsable(field)) {
                    builder.petStoredSp(Integer.parseInt(field));
                }
            }
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
