package org.sokybot.pk2extractor.mediapk2.teleport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.teleport.TeleportData;

/**
 * Extracts Teleport data from teleportdata.txt and teleportlink.txt.
 * Pure extraction with streaming callbacks - no caching or persistence.
 */
public class TeleportDataExtractor implements IExtractor<TeleportData> {

    private static final String NAME = "Teleport Data";
    private static final String DATA_FILE = "teleportdata.txt";
    private static final String LINK_FILE = "teleportlink.txt";

    @Override
    public Class<TeleportData> getDtoClass() {
        return TeleportData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<TeleportData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            // First extract links
            Map<Integer, List<Integer>> links = extractLinks(driver);
            
            // Then extract teleport data with links
            driver.find(DATA_FILE).forEach(teleportdata -> {
                Pk2ExtractorUtils.toCSVRecordStream(teleportdata)
                    .forEach(record -> {
                        TeleportData dto = toTeleportData(record, links);
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

    private Map<Integer, List<Integer>> extractLinks(IPk2Driver driver) {
        Map<Integer, List<Integer>> links = new HashMap<>();
        
        driver.find(LINK_FILE).forEach(jmx -> {
            Pk2ExtractorUtils.toCSVRecordStream(jmx).forEach(record -> {
                String field = record.get(1);
                int id = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
                
                field = record.get(2);
                int link = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
                
                links.computeIfAbsent(id, k -> new ArrayList<>()).add(link);
            });
        });
        
        return links;
    }

    private TeleportData toTeleportData(CSVRecord record, Map<Integer, List<Integer>> links) {
        try {
            String field = record.get(1);
            int refId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
            
            String longId = record.get(2);
            
            field = record.get(3);
            int portalId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
            
            String name = record.get(4); // Name field
            
            return TeleportData.builder()
                .refId(refId)
                .longId(longId)
                .portalId(portalId)
                .name(name)
                .links(links.getOrDefault(refId, new ArrayList<>()))
                .build();
        } catch (Exception e) {
            return null;
        }
    }
}
