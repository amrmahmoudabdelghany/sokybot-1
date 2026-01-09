package org.sokybot.pk2extractor.mediapk2;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.OptionalTeleportData;

/**
 * Extracts optional teleport data from refoptionalteleport.txt.
 * Pure extraction with streaming callbacks.
 */
public class OptionalTeleportExtractor implements IExtractor<OptionalTeleportData> {

    private static final String NAME = "Optional Teleport Data";
    private static final String FILE_PATTERN = "(?i)refoptionalteleport.*\\.txt$";

    @Override
    public Class<OptionalTeleportData> getDtoClass() {
        return OptionalTeleportData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<OptionalTeleportData> listener,
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
                    OptionalTeleportData dto = toOptionalTeleportData(record);
                    if (dto != null && dto.getId() > 0) {
                        counter[0]++;
                        if (listener != null) listener.onExtracted(dto);
                        if (progressListener != null) 
                            progressListener.onProgress(NAME, counter[0], -1, String.valueOf(dto.getId()));
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

    private OptionalTeleportData toOptionalTeleportData(CSVRecord record) {
        try {
            var builder = OptionalTeleportData.builder();
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) builder.id(Integer.parseInt(field));
            builder.objName128(record.get(2));
            builder.zoneName128(record.get(3));
            field = record.get(4);
            if (NumberUtils.isParsable(field)) builder.regionId(Integer.parseInt(field));
            if (record.size() > 7) {
                if (NumberUtils.isParsable(record.get(5))) builder.posX(Float.parseFloat(record.get(5)));
                if (NumberUtils.isParsable(record.get(6))) builder.posZ(Float.parseFloat(record.get(6)));
                if (NumberUtils.isParsable(record.get(7))) builder.posY(Float.parseFloat(record.get(7)));
            }
            if (record.size() > 8 && NumberUtils.isParsable(record.get(8))) 
                builder.worldId(Integer.parseInt(record.get(8)));
            if (record.size() > 12) {
                if (NumberUtils.isParsable(record.get(11))) builder.minLevel(Integer.parseInt(record.get(11)));
                if (NumberUtils.isParsable(record.get(12))) builder.maxLevel(Integer.parseInt(record.get(12)));
            }
            return builder.build();
        } catch (Exception e) { return null; }
    }
}
