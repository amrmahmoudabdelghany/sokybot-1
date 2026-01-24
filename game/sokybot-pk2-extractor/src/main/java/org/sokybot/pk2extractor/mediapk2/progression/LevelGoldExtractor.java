package org.sokybot.pk2extractor.mediapk2.progression;

import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.progression.LevelGoldData;
import org.sokybot.pk2extractor.exception.Pk2MissedResourceException;

/**
 * Extracts level-based gold drop data from levelgold.txt.
 * Defines minimum and maximum gold drops for monsters at each level.
 * Reference: skrillax gold.rs
 */
public class LevelGoldExtractor implements IExtractor<LevelGoldData> {

    private static final String NAME = "Level Gold";
    private static final String LEVEL_GOLD_FILE = "(?i)levelgold.txt";

    @Override
    public Class<LevelGoldData> getDtoClass() {
        return LevelGoldData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<LevelGoldData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            // Find and parse levelgold.txt
            driver.findFirst(LEVEL_GOLD_FILE)
                .map(jmx -> Pk2ExtractorUtils.toCSVRecordStream(jmx, StandardCharsets.UTF_16))
                .orElseThrow(() -> new Pk2MissedResourceException(
                    "Could not find levelgold.txt", "levelgold.txt"))
                .forEach(record -> {
                    LevelGoldData dto = toLevelGoldData(record);
                    if (dto != null) {
                        counter[0]++;
                        
                        if (listener != null) {
                            listener.onExtracted(dto);
                        }
                        
                        if (progressListener != null) {
                            progressListener.onProgress(NAME, counter[0], -1, 
                                "Level " + dto.getLevel());
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

    /**
     * Parse levelgold.txt record.
     * Format: level \t min_gold \t max_gold
     */
    private LevelGoldData toLevelGoldData(CSVRecord record) {
        try {
            if (record.size() < 3) {
                return null;
            }
            
            String field0 = record.get(0);
            if (!NumberUtils.isParsable(field0)) {
                return null;
            }
            
            int level = Integer.parseInt(field0);
            int minGold = NumberUtils.isParsable(record.get(1)) ? 
                Integer.parseInt(record.get(1)) : 0;
            int maxGold = NumberUtils.isParsable(record.get(2)) ? 
                Integer.parseInt(record.get(2)) : 0;
            
            return LevelGoldData.builder()
                .level(level)
                .minGold(minGold)
                .maxGold(maxGold)
                .build();
                
        } catch (Exception e) {
            return null;
        }
    }
}
