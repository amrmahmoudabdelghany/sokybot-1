package org.sokybot.pk2extractor.mediapk2.gameinfo;

import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.progression.LvlData;
import org.sokybot.pk2extractor.exception.Pk2MissedResourceException;

/**
 * Extracts Level/EXP data from pk2 files.
 * Pure extraction with streaming callbacks - no caching or persistence.
 */
public class LvlDataExtractor implements IExtractor<LvlData> {

    private static final String NAME = "Level Data";
    private static final String FILE_NAME = "leveldata.txt";

    @Override
    public Class<LvlData> getDtoClass() {
        return LvlData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<LvlData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0}; // Mutable counter for lambda
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1); // Unknown total at start
            }
            
            driver.findFirst(FILE_NAME)
                .map(jmx -> Pk2ExtractorUtils.toCSVRecordStream(jmx, StandardCharsets.UTF_16))
                .orElseThrow(() -> new Pk2MissedResourceException(
                    "Could not find " + FILE_NAME, FILE_NAME))
                .forEach(record -> {
                    LvlData dto = toLvlData(record);
                    if (dto != null) {
                        counter[0]++;
                        
                        // Notify listener immediately
                        if (listener != null) {
                            listener.onExtracted(dto);
                        }
                        
                        // Report progress on every item
                        if (progressListener != null) {
                            progressListener.onProgress(NAME, counter[0], -1, 
                                String.valueOf(dto.getLevel()));
                        }
                    }
                });
            
            // Notify completion
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

    private LvlData toLvlData(CSVRecord record) {
        try {
            String levelField = record.get(0);
            String expField = record.get(1);
            
            int level = NumberUtils.isParsable(levelField) 
                ? Integer.parseInt(levelField) : 0;
            long exp = NumberUtils.isParsable(expField) 
                ? Long.parseLong(expField) : Long.MAX_VALUE;
            
            return LvlData.builder()
                .level(level)
                .experience(exp)
                .build();
        } catch (Exception e) {
            return null; // Skip malformed records
        }
    }
}
