package org.sokybot.pk2extractor.mediapk2.character;

import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.character.NpcPositionData;
import org.sokybot.pk2extractor.exception.Pk2MissedResourceException;

/**
 * Extracts NPC spawn position data from NpcPos.txt in pk2 files.
 * Contains world coordinates where each NPC type spawns.
 * Reference: skrillax npc_pos.rs
 */
public class NpcPositionExtractor implements IExtractor<NpcPositionData> {

    private static final String NAME = "NPC Position";
    private static final String NPC_POS_FILE = "(?i)npcpos.txt";

    @Override
    public Class<NpcPositionData> getDtoClass() {
        return NpcPositionData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<NpcPositionData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            // Find and parse NpcPos.txt
            driver.findFirst(NPC_POS_FILE)
                .map(jmx -> Pk2ExtractorUtils.toCSVRecordStream(jmx, StandardCharsets.UTF_16))
                .orElseThrow(() -> new Pk2MissedResourceException(
                    "Could not find npcpos.txt", "npcpos.txt"))
                .forEach(record -> {
                    NpcPositionData dto = toNpcPositionData(record);
                    if (dto != null) {
                        counter[0]++;
                        
                        if (listener != null) {
                            listener.onExtracted(dto);
                        }
                        
                        if (progressListener != null) {
                            progressListener.onProgress(NAME, counter[0], -1, 
                                "NPC " + dto.getNpcId() + " @ " + dto.getRegion());
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

    /**
     * Parse NpcPos.txt record.
     * Format: npc_id \t region \t x \t y \t z
     */
    private NpcPositionData toNpcPositionData(CSVRecord record) {
        try {
            if (record.size() < 5) {
                return null;
            }
            
            String field0 = record.get(0);
            if (!NumberUtils.isParsable(field0)) {
                return null;
            }
            
            int npcId = Integer.parseInt(field0);
            
            // Region can be negative (signed), cast accordingly
            String field1 = record.get(1);
            int region = NumberUtils.isParsable(field1) ? 
                (short) Integer.parseInt(field1) & 0xFFFF : 0;
            
            float x = NumberUtils.isParsable(record.get(2)) ? 
                Float.parseFloat(record.get(2)) : 0f;
            float y = NumberUtils.isParsable(record.get(3)) ? 
                Float.parseFloat(record.get(3)) : 0f;
            float z = NumberUtils.isParsable(record.get(4)) ? 
                Float.parseFloat(record.get(4)) : 0f;
            
            return NpcPositionData.builder()
                .npcId(npcId)
                .region(region)
                .x(x)
                .y(y)
                .z(z)
                .build();
                
        } catch (Exception e) {
            return null;
        }
    }
}
