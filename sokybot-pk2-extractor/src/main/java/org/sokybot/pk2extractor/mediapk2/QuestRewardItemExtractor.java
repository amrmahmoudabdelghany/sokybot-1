package org.sokybot.pk2extractor.mediapk2;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.QuestRewardItemData;

/**
 * Extracts quest item reward data from refquestrewarditem.txt.
 * Pure extraction with streaming callbacks.
 */
public class QuestRewardItemExtractor implements IExtractor<QuestRewardItemData> {

    private static final String NAME = "Quest Item Reward Data";
    private static final String FILE_PATTERN = "(?i)refquestrewarditem.*\\.txt$";

    @Override
    public Class<QuestRewardItemData> getDtoClass() {
        return QuestRewardItemData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<QuestRewardItemData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) progressListener.onStart(NAME, -1);
            
            driver.find(FILE_PATTERN).stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 7 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    QuestRewardItemData dto = toQuestRewardItemData(record);
                    if (dto != null && dto.getQuestId() > 0) {
                        counter[0]++;
                        if (listener != null) listener.onExtracted(dto);
                        if (progressListener != null) 
                            progressListener.onProgress(NAME, counter[0], -1, dto.getItemCodeName());
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

    private QuestRewardItemData toQuestRewardItemData(CSVRecord record) {
        try {
            var builder = QuestRewardItemData.builder();
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) builder.questId(Integer.parseInt(field));
            builder.questCodeName(record.get(1));
            field = record.get(2);
            if (NumberUtils.isParsable(field)) builder.rewardType(Byte.parseByte(field));
            builder.itemCodeName(record.get(3));
            builder.optionalItemCode(record.get(4));
            field = record.get(5);
            if (NumberUtils.isParsable(field)) builder.optionalItemCount(Integer.parseInt(field));
            field = record.get(6);
            if (NumberUtils.isParsable(field)) builder.achieveQuantity(Integer.parseInt(field));
            if (record.size() > 7) builder.rentItemCodeName(record.get(7));
            return builder.build();
        } catch (Exception e) { return null; }
    }
}
