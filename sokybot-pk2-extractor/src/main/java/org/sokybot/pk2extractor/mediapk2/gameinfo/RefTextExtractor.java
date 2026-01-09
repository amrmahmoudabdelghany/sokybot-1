package org.sokybot.pk2extractor.mediapk2;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.RefTextData;

/**
 * Extracts localization text data from textdata_*.txt files.
 * Pure extraction with streaming callbacks.
 */
public class RefTextExtractor implements IExtractor<RefTextData> {

    private static final String NAME = "Localization Text";
    private static final String FILE_PATTERN = "(?i)textdata_.*\\.txt$";

    @Override
    public Class<RefTextData> getDtoClass() {
        return RefTextData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<RefTextData> listener,
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
                    RefTextData dto = toRefTextData(record);
                    if (dto != null && dto.getNameStrId() != null && !dto.getNameStrId().isBlank()) {
                        counter[0]++;
                        
                        if (listener != null) {
                            listener.onExtracted(dto);
                        }
                        
                        if (progressListener != null) {
                            progressListener.onProgress(NAME, counter[0], -1, dto.getNameStrId());
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

    private RefTextData toRefTextData(CSVRecord record) {
        try {
            var builder = RefTextData.builder();
            
            // Field 0: Service
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) {
                builder.service(Integer.parseInt(field));
            }
            
            // Identify key index (usually starts with SN_)
            int keyIndex = 1;
            if (record.size() > 2 && !record.get(1).startsWith("SN_") && record.get(2).startsWith("SN_")) {
                keyIndex = 2;
            }
            
            builder.nameStrId(record.get(keyIndex));
            
            // Get data from last non-empty column
            int dataIndex = record.size() - 1;
            while (dataIndex > keyIndex && (record.get(dataIndex) == null || record.get(dataIndex).isBlank())) {
                dataIndex--;
            }
            
            if (dataIndex > keyIndex) {
                builder.data(record.get(dataIndex));
            } else {
                builder.data(record.get(keyIndex));
            }
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
