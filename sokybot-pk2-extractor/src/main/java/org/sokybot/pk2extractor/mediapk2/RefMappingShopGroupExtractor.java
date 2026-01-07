package org.sokybot.pk2extractor.mediapk2;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.RefMappingShopGroupData;

/**
 * Extracts shop group mapping data from refmappingshopgroup.txt.
 * Pure extraction with streaming callbacks.
 */
public class RefMappingShopGroupExtractor implements IExtractor<RefMappingShopGroupData> {

    private static final String NAME = "Shop Group Mapping Data";
    private static final String FILE_PATTERN = "(?i)refmappingshopgroup.*\\.txt$";

    @Override
    public Class<RefMappingShopGroupData> getDtoClass() {
        return RefMappingShopGroupData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<RefMappingShopGroupData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) progressListener.onStart(NAME, -1);
            
            driver.find(FILE_PATTERN).stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 3 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    RefMappingShopGroupData dto = toMappingData(record);
                    if (dto != null && dto.getGroupCodeName() != null && !dto.getGroupCodeName().isBlank()) {
                        counter[0]++;
                        if (listener != null) listener.onExtracted(dto);
                        if (progressListener != null) 
                            progressListener.onProgress(NAME, counter[0], -1, dto.getGroupCodeName());
                    }
                });
            
            long duration = System.currentTimeMillis() - startTime;
            if (listener != null) listener.onComplete(counter[0]);
            if (progressListener != null) progressListener.onComplete(NAME, counter[0], duration);
            
        } catch (Exception e) {
            if (listener != null) listener.onError(e);
            if (progressListener != null) progressListener.onError(NAME, e);
        }
    }

    private RefMappingShopGroupData toMappingData(CSVRecord record) {
        try {
            var builder = RefMappingShopGroupData.builder();
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) builder.country(Integer.parseInt(field));
            builder.groupCodeName(record.get(2));
            builder.shopCodeName(record.get(3));
            return builder.build();
        } catch (Exception e) { return null; }
    }
}
