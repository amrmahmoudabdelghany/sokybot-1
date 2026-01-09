package org.sokybot.pk2extractor.mediapk2.quest;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.quest.EventRewardData;

/**
 * Extracts event reward item data from refeventrewarditems.txt.
 * Pure extraction with streaming callbacks.
 */
public class EventRewardExtractor implements IExtractor<EventRewardData> {

    private static final String NAME = "Event Reward Data";
    private static final String FILE_PATTERN = "(?i)refeventrewarditems.*\\.txt$";

    @Override
    public Class<EventRewardData> getDtoClass() {
        return EventRewardData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<EventRewardData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            driver.find(FILE_PATTERN).stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 3 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    EventRewardData dto = toEventRewardData(record);
                    if (dto != null && dto.getEventId() > 0) {
                        counter[0]++;
                        
                        if (listener != null) {
                            listener.onExtracted(dto);
                        }
                        
                        if (progressListener != null) {
                            progressListener.onProgress(NAME, counter[0], -1, dto.getEventCodeName());
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

    private EventRewardData toEventRewardData(CSVRecord record) {
        try {
            var builder = EventRewardData.builder();
            
            // Field 0: Event ID
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) {
                builder.eventId(Integer.parseInt(field));
            }
            
            // Field 1: Event Code Name
            builder.eventCodeName(record.get(1));
            
            // Field 2: Item Code Name
            builder.itemCodeName(record.get(2));
            
            // Field 3: Amount
            field = record.get(3);
            if (NumberUtils.isParsable(field)) {
                builder.itemAmount(Integer.parseInt(field));
            }
            
            // Field 7: Min Level
            if (record.size() > 7) {
                field = record.get(7);
                if (NumberUtils.isParsable(field)) {
                    builder.minRequiredLevel(Integer.parseInt(field));
                }
            }
            
            // Field 8: Max Level
            if (record.size() > 8) {
                field = record.get(8);
                if (NumberUtils.isParsable(field)) {
                    builder.maxRequiredLevel(Integer.parseInt(field));
                }
            }
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
