package org.sokybot.pk2extractor.mediapk2;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.MasteryData;
import org.sokybot.pk2extractor.exception.Pk2MissedResourceException;

/**
 * Extracts Mastery data from skillmasterydata.txt.
 * Pure extraction with streaming callbacks - no caching or persistence.
 */
public class MasteryDataExtractor implements IExtractor<MasteryData> {

    private static final String NAME = "Mastery Data";
    private static final String FILE_NAME = "skillmasterydata.txt";

    @Override
    public Class<MasteryData> getDtoClass() {
        return MasteryData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<MasteryData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            driver.findFirst(FILE_NAME)
                .orElseThrow(() -> new Pk2MissedResourceException(
                    "Could not find " + FILE_NAME, FILE_NAME));
            
            driver.find(FILE_NAME).forEach(jmx -> {
                Pk2ExtractorUtils.toCSVRecordStream(jmx)
                    .filter(record -> record.size() >= 3)
                    .filter(record -> NumberUtils.isParsable(record.get(0)))
                    .filter(record -> !record.get(2).contains("xxx"))
                    .forEach(record -> {
                        MasteryData dto = toMasteryData(record);
                        if (dto != null) {
                            counter[0]++;
                            
                            if (listener != null) {
                                listener.onExtracted(dto);
                            }
                            
                            if (progressListener != null) {
                                progressListener.onProgress(NAME, counter[0], -1, 
                                    String.valueOf(dto.getMasteryId()));
                            }
                        }
                    });
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

    private MasteryData toMasteryData(CSVRecord record) {
        try {
            int masteryId = Integer.parseInt(record.get(0));
            // Level field (if available, otherwise default)
            int level = record.size() > 1 && NumberUtils.isParsable(record.get(1)) 
                ? Integer.parseInt(record.get(1)) : 0;
            // Required SP (if available)
            long requiredSp = record.size() > 3 && NumberUtils.isParsable(record.get(3)) 
                ? Long.parseLong(record.get(3)) : 0;
            
            return MasteryData.builder()
                .masteryId(masteryId)
                .level(level)
                .requiredSp(requiredSp)
                .build();
        } catch (Exception e) {
            return null;
        }
    }
}
