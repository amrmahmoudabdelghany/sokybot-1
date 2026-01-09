package org.sokybot.pk2extractor.mediapk2.shop;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.shop.RefShopTabData;

/**
 * Extracts shop tab data from refshoptab.txt.
 * Pure extraction with streaming callbacks.
 */
public class RefShopTabExtractor implements IExtractor<RefShopTabData> {

    private static final String NAME = "Shop Tab Data";
    private static final String FILE_PATTERN = "(?i)refshoptab.*\\.txt$";

    @Override
    public Class<RefShopTabData> getDtoClass() {
        return RefShopTabData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<RefShopTabData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) progressListener.onStart(NAME, -1);
            
            driver.find(FILE_PATTERN).stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 5 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    RefShopTabData dto = toRefShopTabData(record);
                    if (dto != null && dto.getId() > 0) {
                        counter[0]++;
                        if (listener != null) listener.onExtracted(dto);
                        if (progressListener != null) 
                            progressListener.onProgress(NAME, counter[0], -1, dto.getCodeName());
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

    private RefShopTabData toRefShopTabData(CSVRecord record) {
        try {
            var builder = RefShopTabData.builder();
            if (NumberUtils.isParsable(record.get(0))) builder.service(Integer.parseInt(record.get(0)));
            if (NumberUtils.isParsable(record.get(1))) builder.country(Integer.parseInt(record.get(1)));
            if (NumberUtils.isParsable(record.get(2))) builder.id(Integer.parseInt(record.get(2)));
            builder.codeName(record.get(3));
            builder.refTabGroupCodeName(record.get(4));
            builder.strID128_Tab(record.get(5));
            return builder.build();
        } catch (Exception e) { return null; }
    }
}
