package org.sokybot.pk2extractor.mediapk2;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.PackageItemData;

/**
 * Extracts package item data from refpackageitem.txt.
 * Pure extraction with streaming callbacks.
 */
public class PackageItemExtractor implements IExtractor<PackageItemData> {

    private static final String NAME = "Package Item Data";
    private static final String FILE_PATTERN = "(?i)refpackageitem.*\\.txt$";

    @Override
    public Class<PackageItemData> getDtoClass() {
        return PackageItemData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<PackageItemData> listener,
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
                    PackageItemData dto = toPackageItemData(record);
                    if (dto != null && dto.getPackageCodeName() != null && !dto.getPackageCodeName().isBlank()) {
                        counter[0]++;
                        
                        if (listener != null) {
                            listener.onExtracted(dto);
                        }
                        
                        if (progressListener != null) {
                            progressListener.onProgress(NAME, counter[0], -1, dto.getPackageCodeName());
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

    private PackageItemData toPackageItemData(CSVRecord record) {
        try {
            var builder = PackageItemData.builder();
            
            // Field 2: Package code name
            builder.packageCodeName(record.get(2));
            
            // Field 3: Item code name
            builder.itemCodeName(record.get(3));
            
            // Field 4: Opt level
            String field = record.get(4);
            if (NumberUtils.isParsable(field)) {
                builder.optLevel(Byte.parseByte(field));
            }
            
            // Field 5: Variance
            field = record.get(5);
            if (NumberUtils.isParsable(field)) {
                builder.variance(Long.parseLong(field));
            }
            
            // Field 6: Durability
            if (record.size() > 6) {
                field = record.get(6);
                if (NumberUtils.isParsable(field)) {
                    builder.durability(Integer.parseInt(field));
                }
            }
            
            // Field 7: Quantity
            if (record.size() > 7) {
                field = record.get(7);
                if (NumberUtils.isParsable(field)) {
                    builder.quantity(Integer.parseInt(field));
                }
            }
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
