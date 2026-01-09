package org.sokybot.pk2extractor.mediapk2.teleport;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.teleport.TeleportLinkData;

/**
 * Extracts teleport link data from teleportlink.txt.
 * Pure extraction with streaming callbacks.
 */
public class TeleportLinkExtractor implements IExtractor<TeleportLinkData> {

    private static final String NAME = "Teleport Link Data";
    private static final String FILE_PATTERN = "(?i)teleportlink.*\\.txt$";

    @Override
    public Class<TeleportLinkData> getDtoClass() {
        return TeleportLinkData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<TeleportLinkData> listener,
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
                    TeleportLinkData dto = toTeleportLinkData(record);
                    if (dto != null && dto.getOwnerTeleportId() > 0) {
                        counter[0]++;
                        
                        if (listener != null) {
                            listener.onExtracted(dto);
                        }
                        
                        if (progressListener != null) {
                            progressListener.onProgress(NAME, counter[0], -1, 
                                String.valueOf(dto.getOwnerTeleportId()));
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

    private TeleportLinkData toTeleportLinkData(CSVRecord record) {
        try {
            var builder = TeleportLinkData.builder();
            
            // Field 1: Owner teleport ID
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.ownerTeleportId(Integer.parseInt(field));
            }
            
            // Field 2: Target teleport ID
            field = record.get(2);
            if (NumberUtils.isParsable(field)) {
                builder.targetTeleportId(Integer.parseInt(field));
            }
            
            // Field 3: Fee
            field = record.get(3);
            if (NumberUtils.isParsable(field)) {
                builder.fee(Integer.parseInt(field));
            }
            
            // Field 4: Restrict bind method
            if (record.size() > 4) {
                field = record.get(4);
                if (NumberUtils.isParsable(field)) {
                    builder.restrictBindMethod(Byte.parseByte(field));
                }
            }
            
            // Field 5: Check result
            if (record.size() > 5) {
                field = record.get(5);
                if (NumberUtils.isParsable(field)) {
                    builder.checkResult(Byte.parseByte(field));
                }
            }
            
            // Restriction fields
            if (record.size() > 8) {
                builder.restrict1(parseIntOrZero(record.get(6)));
                builder.data1_1(parseIntOrZero(record.get(7)));
                builder.data1_2(parseIntOrZero(record.get(8)));
            }
            
            if (record.size() > 11) {
                builder.restrict2(parseIntOrZero(record.get(9)));
                builder.data2_1(parseIntOrZero(record.get(10)));
                builder.data2_2(parseIntOrZero(record.get(11)));
            }
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
    
    private int parseIntOrZero(String value) {
        return NumberUtils.isParsable(value) ? Integer.parseInt(value) : 0;
    }
}
