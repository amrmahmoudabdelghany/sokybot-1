package org.sokybot.pk2extractor.mediapk2.item.magic;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.item.magic.MagicOptionAssignmentData;

/**
 * Extracts magic option assignment data from refmagicoptassign.txt.
 * Pure extraction with streaming callbacks.
 */
public class MagicOptionAssignmentExtractor implements IExtractor<MagicOptionAssignmentData> {

    private static final String NAME = "Magic Option Assignment Data";
    private static final String FILE_PATTERN = "(?i)(ref)?magicopt.*assign.*\\.txt$";

    @Override
    public Class<MagicOptionAssignmentData> getDtoClass() {
        return MagicOptionAssignmentData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<MagicOptionAssignmentData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) progressListener.onStart(NAME, -1);
            
            driver.find(FILE_PATTERN).stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 4 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    MagicOptionAssignmentData dto = toMagicOptionAssignmentData(record);
                    if (dto != null && (dto.getRace() != 0 || dto.getTypeId3() != 0)) {
                        counter[0]++;
                        if (listener != null) listener.onExtracted(dto);
                        if (progressListener != null) 
                            progressListener.onProgress(NAME, counter[0], -1, 
                                String.valueOf(dto.getTypeId3()));
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

    private MagicOptionAssignmentData toMagicOptionAssignmentData(CSVRecord record) {
        try {
            var builder = MagicOptionAssignmentData.builder();
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) builder.race(Byte.parseByte(field));
            field = record.get(2);
            if (NumberUtils.isParsable(field)) builder.typeId3(Byte.parseByte(field));
            field = record.get(3);
            if (NumberUtils.isParsable(field)) builder.typeId4(Byte.parseByte(field));
            
            List<String> options = new ArrayList<>();
            for (int i = 4; i < record.size(); i++) {
                String opt = record.get(i);
                if (opt != null && !opt.isBlank() && !opt.equals("xxx") && !opt.equals("NULL")) {
                    options.add(opt);
                }
            }
            builder.availableMagicOptions(options);
            return builder.build();
        } catch (Exception e) { return null; }
    }
}
