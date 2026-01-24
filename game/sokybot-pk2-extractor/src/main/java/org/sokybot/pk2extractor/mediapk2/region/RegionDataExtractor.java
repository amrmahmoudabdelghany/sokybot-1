package org.sokybot.pk2extractor.mediapk2.region;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.region.RegionData;

/**
 * Extracts region/zone data from regioninfo.txt files.
 * Pure extraction with streaming callbacks.
 */
public class RegionDataExtractor implements IExtractor<RegionData> {

    private static final String NAME = "Region Data";
    private static final String FILE_PATTERN = "(?i)regioninfo.*\\.txt$";

    @Override
    public Class<RegionData> getDtoClass() {
        return RegionData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<RegionData> listener,
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
                    RegionData dto = toRegionData(record);
                    if (dto != null && dto.getId() > 0) {
                        counter[0]++;
                        
                        if (listener != null) {
                            listener.onExtracted(dto);
                        }
                        
                        if (progressListener != null) {
                            progressListener.onProgress(NAME, counter[0], -1, dto.getLongId());
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

    private RegionData toRegionData(CSVRecord record) {
        try {
            var builder = RegionData.builder();
            
            // Field 1: ID
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.id(Integer.parseInt(field));
            }
            
            // Field 2: Long ID
            builder.longId(record.get(2));
            
            // Field 3: Name
            builder.name(record.get(3));
            
            // Field 4: Region type
            field = record.get(4);
            if (NumberUtils.isParsable(field)) {
                builder.type(Integer.parseInt(field));
            }
            
            // Field 5: Area ID
            if (record.size() > 5) {
                field = record.get(5);
                if (NumberUtils.isParsable(field)) {
                    builder.areaId(Integer.parseInt(field));
                }
            }
            
            // Field 6: PvP flag
            if (record.size() > 6) {
                field = record.get(6);
                builder.isPvP(NumberUtils.isParsable(field) && 
                              BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
            // Field 7: Safe zone flag
            if (record.size() > 7) {
                field = record.get(7);
                builder.isSafe(NumberUtils.isParsable(field) && 
                               BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
            // Field 8-9: Min/Max level
            if (record.size() > 8) {
                field = record.get(8);
                if (NumberUtils.isParsable(field)) {
                    builder.minLevel(Integer.parseInt(field));
                }
            }
            
            if (record.size() > 9) {
                field = record.get(9);
                if (NumberUtils.isParsable(field)) {
                    builder.maxLevel(Integer.parseInt(field));
                }
            }
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
