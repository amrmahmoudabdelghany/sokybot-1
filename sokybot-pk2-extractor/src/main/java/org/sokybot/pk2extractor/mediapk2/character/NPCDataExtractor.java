package org.sokybot.pk2extractor.mediapk2;

import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.NPCData;
import org.sokybot.pk2extractor.dto.NPCTypeData;
import org.sokybot.pk2extractor.exception.Pk2MissedResourceException;

/**
 * Extracts NPC data from pk2 files.
 * Pure extraction with streaming callbacks - no caching or persistence.
 */
public class NPCDataExtractor implements IExtractor<NPCData> {

    private static final String NAME = "NPC Data";
    private static final String INDEX_FILE = "characterdata.txt";

    @Override
    public Class<NPCData> getDtoClass() {
        return NPCData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<NPCData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            // Find character data index file and get list of NPC files to process
            driver.findFirst(INDEX_FILE)
                .map(jmx -> Pk2ExtractorUtils.toString(jmx, StandardCharsets.UTF_16.name()))
                .map(Pk2ExtractorUtils::toLines)
                .orElseThrow(() -> new Pk2MissedResourceException(
                    "Could not find " + INDEX_FILE, INDEX_FILE))
                .flatMap(npcFileName -> driver.find("(?i)" + npcFileName).stream())
                .flatMap(jmx -> Pk2ExtractorUtils.toCSVRecordStream(jmx, StandardCharsets.UTF_16))
                .forEach(record -> {
                    NPCData dto = toNPCData(record);
                    if (dto != null) {
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

    private NPCData toNPCData(CSVRecord record) {
        try {
            String field = record.get(1);
            int refId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
            
            String longId = record.get(2);
            String name = record.get(5); // Direct name field
            
            field = record.get(57);
            int lvl = NumberUtils.isParsable(field) ? Integer.parseInt(field) : 0;
            
            field = record.get(59);
            int hp = NumberUtils.isParsable(field) ? Integer.parseInt(field) : 0;
            
            // Type flag parsing
            String typeFlag = record.get(10) + record.get(11) + record.get(12) + 
                             record.get(14) + record.get(15);
            int typeValue = NumberUtils.isParsable(typeFlag) ? Integer.parseInt(typeFlag) : -1;
            
            return NPCData.builder()
                .refId(refId)
                .longId(longId)
                .name(name)
                .level(lvl)
                .HP(hp)
                .type(NPCTypeData.of(typeValue))
                .build();
        } catch (Exception e) {
            return null;
        }
    }
}
