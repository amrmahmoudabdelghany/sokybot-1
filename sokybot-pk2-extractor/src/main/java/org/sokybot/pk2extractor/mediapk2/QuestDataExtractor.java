package org.sokybot.pk2extractor.mediapk2;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.QuestData;

/**
 * Extracts quest data from questdata.txt files.
 * Pure extraction with streaming callbacks.
 */
public class QuestDataExtractor implements IExtractor<QuestData> {

    private static final String NAME = "Quest Data";
    private static final String FILE_PATTERN = "questdata.txt$";

    @Override
    public Class<QuestData> getDtoClass() {
        return QuestData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<QuestData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            driver.find(FILE_PATTERN).stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 20 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    QuestData dto = toQuestData(record);
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

    private QuestData toQuestData(CSVRecord record) {
        try {
            var builder = QuestData.builder();
            
            // Field 1: ID
            String field = record.get(1);
            if (NumberUtils.isParsable(field)) {
                builder.id(Integer.parseInt(field));
            }
            
            // Field 2: Long ID
            builder.longId(record.get(2));
            
            // Field 3: Name
            builder.name(record.get(3));
            
            // Field 4: Quest type
            field = record.get(4);
            if (NumberUtils.isParsable(field)) {
                builder.type(Integer.parseInt(field));
            }
            
            // Field 5-6: Min/Max level
            field = record.get(5);
            if (NumberUtils.isParsable(field)) {
                builder.minLevel(Integer.parseInt(field));
            }
            
            field = record.get(6);
            if (NumberUtils.isParsable(field)) {
                builder.maxLevel(Integer.parseInt(field));
            }
            
            // Field 7: Repeat count
            field = record.get(7);
            if (NumberUtils.isParsable(field)) {
                builder.repeatCount(Integer.parseInt(field));
            }
            
            // Field 8: Party flag
            if (record.size() > 8) {
                field = record.get(8);
                if (NumberUtils.isParsable(field)) {
                    builder.isParty(BooleanUtils.toBoolean(Byte.valueOf(field)));
                }
            }
            
            // Field 9: NPC reference
            if (record.size() > 9) {
                field = record.get(9);
                if (NumberUtils.isParsable(field)) {
                    builder.npcRefId(Integer.parseInt(field));
                }
            }
            
            // Reward fields
            if (record.size() > 15) {
                field = record.get(15);
                if (NumberUtils.isParsable(field)) {
                    builder.rewardExp(Integer.parseInt(field));
                }
            }
            
            if (record.size() > 16) {
                field = record.get(16);
                if (NumberUtils.isParsable(field)) {
                    builder.rewardGold(Integer.parseInt(field));
                }
            }
            
            if (record.size() > 17) {
                field = record.get(17);
                if (NumberUtils.isParsable(field)) {
                    builder.rewardSkillPoint(Integer.parseInt(field));
                }
            }
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
