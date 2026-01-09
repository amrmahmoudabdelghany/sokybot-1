package org.sokybot.pk2extractor.mediapk2;

import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.PortalData;

/**
 * Extracts Portal data from teleportbuilding.txt.
 * Pure extraction with streaming callbacks - no caching or persistence.
 */
public class PortalDataExtractor implements IExtractor<PortalData> {

    private static final String NAME = "Portal Data";
    private static final String FILE_NAME = "teleportbuilding.txt";

    @Override
    public Class<PortalData> getDtoClass() {
        return PortalData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<PortalData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            driver.find(FILE_NAME).forEach(teleportbuilding -> {
                Pk2ExtractorUtils.toCSVRecordStream(teleportbuilding)
                    .forEach(record -> {
                        PortalData dto = toPortalData(record);
                        if (dto != null) {
                            counter[0]++;
                            
                            if (listener != null) {
                                listener.onExtracted(dto);
                            }
                            
                            if (progressListener != null) {
                                progressListener.onProgress(NAME, counter[0], -1, dto.getLongId());
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

    private PortalData toPortalData(org.apache.commons.csv.CSVRecord record) {
        try {
            String field = record.get(1);
            int refId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
            String longId = record.get(2);
            String name = record.get(5); // Name field
            
            return PortalData.builder()
                .refId(refId)
                .longId(longId)
                .name(name)
                .build();
        } catch (Exception e) {
            return null;
        }
    }
}
