package org.sokybot.pk2extractor.mediapk2;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.MagicOptionData;

/**
 * Extracts magic option (blue stat) data from magicoption.txt files.
 * Pure extraction with streaming callbacks.
 */
public class MagicOptionExtractor implements IExtractor<MagicOptionData> {

    private static final String NAME = "Magic Option Data";
    private static final String FILE_PATTERN = "magicoption.*\\.txt$";

    @Override
    public Class<MagicOptionData> getDtoClass() {
        return MagicOptionData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<MagicOptionData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            driver.find(FILE_PATTERN).stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 10 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    MagicOptionData dto = toMagicOptionData(record);
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

    private MagicOptionData toMagicOptionData(CSVRecord record) {
        try {
            var builder = MagicOptionData.builder();
            
            // Field 1: ID
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.id(Integer.parseInt(field));
            }
            
            // Field 2: Long ID
            builder.longId(record.get(2));
            
            // Field 3: Name
            builder.name(record.get(3));
            
            // Field 4: Attribute type
            field = record.get(4);
            if (NumberUtils.isParsable(field)) {
                builder.attributeType(Integer.parseInt(field));
            }
            
            // Field 5-6: Min/Max value
            field = record.get(5);
            if (NumberUtils.isParsable(field)) {
                builder.minValue(Integer.parseInt(field));
            }
            
            field = record.get(6);
            if (NumberUtils.isParsable(field)) {
                builder.maxValue(Integer.parseInt(field));
            }
            
            // Field 7: Degree
            if (record.size() > 7) {
                field = record.get(7);
                if (NumberUtils.isParsable(field)) {
                    builder.degree(Integer.parseInt(field));
                }
            }
            
            // Field 8-11: Applicable item types
            if (record.size() > 8) {
                field = record.get(8);
                builder.isWeapon(NumberUtils.isParsable(field) && 
                                 BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
            if (record.size() > 9) {
                field = record.get(9);
                builder.isArmor(NumberUtils.isParsable(field) && 
                                BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
            if (record.size() > 10) {
                field = record.get(10);
                builder.isAccessory(NumberUtils.isParsable(field) && 
                                    BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
            if (record.size() > 11) {
                field = record.get(11);
                builder.isShield(NumberUtils.isParsable(field) && 
                                 BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
