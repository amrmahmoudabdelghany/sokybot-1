package org.sokybot.pk2extractor.mediapk2;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.ShopGroupData;

/**
 * Extracts shop group data from refshopgroup.txt.
 * Pure extraction with streaming callbacks.
 */
public class ShopGroupExtractor implements IExtractor<ShopGroupData> {

    private static final String NAME = "Shop Group Data";
    private static final String FILE_PATTERN = "(?i)refshopgroup.*\\.txt$";

    @Override
    public Class<ShopGroupData> getDtoClass() {
        return ShopGroupData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<ShopGroupData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            driver.find(FILE_PATTERN).stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 4 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    ShopGroupData dto = toShopGroupData(record);
                    if (dto != null && dto.getId() > 0) {
                        counter[0]++;
                        
                        if (listener != null) {
                            listener.onExtracted(dto);
                        }
                        
                        if (progressListener != null) {
                            progressListener.onProgress(NAME, counter[0], -1, dto.getCodeName());
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

    private ShopGroupData toShopGroupData(CSVRecord record) {
        try {
            var builder = ShopGroupData.builder();
            
            // Field 1: Country
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.country(Integer.parseInt(field));
            }
            
            // Field 2: ID
            field = record.get(2);
            if (NumberUtils.isParsable(field)) {
                builder.id(Integer.parseInt(field));
            }
            
            // Field 3: CodeName
            builder.codeName(record.get(3));
            
            // Field 4: RefNpcCodeName
            builder.refNpcCodeName(record.get(4));
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
