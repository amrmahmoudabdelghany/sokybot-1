package org.sokybot.pk2extractor.mediapk2.quest;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.quest.QuestRewardData;

/**
 * Extracts quest reward data from refquestreward.txt.
 * Pure extraction with streaming callbacks.
 */
public class QuestRewardExtractor implements IExtractor<QuestRewardData> {

    private static final String NAME = "Quest Reward Data";
    private static final String FILE_PATTERN = "(?i)refquestreward.*\\.txt$";

    @Override
    public Class<QuestRewardData> getDtoClass() {
        return QuestRewardData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<QuestRewardData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) progressListener.onStart(NAME, -1);
            
            driver.find(FILE_PATTERN).stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 5 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    QuestRewardData dto = toQuestRewardData(record);
                    if (dto != null && dto.getQuestId() > 0) {
                        counter[0]++;
                        if (listener != null) listener.onExtracted(dto);
                        if (progressListener != null) 
                            progressListener.onProgress(NAME, counter[0], -1, dto.getQuestCodeName());
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

    private QuestRewardData toQuestRewardData(CSVRecord record) {
        try {
            var builder = QuestRewardData.builder();
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) builder.questId(Integer.parseInt(field));
            builder.questCodeName(record.get(1));
            field = record.get(2);
            if (NumberUtils.isParsable(field)) builder.isView(BooleanUtils.toBoolean(Byte.valueOf(field)));
            if (record.size() > 4) {
                field = record.get(4);
                if (NumberUtils.isParsable(field)) builder.isItemReward(BooleanUtils.toBoolean(Byte.valueOf(field)));
            }
            if (record.size() > 18) {
                if (NumberUtils.isParsable(record.get(10))) builder.gold(Integer.parseInt(record.get(10)));
                if (NumberUtils.isParsable(record.get(11))) builder.exp(Integer.parseInt(record.get(11)));
                if (NumberUtils.isParsable(record.get(12))) builder.spExp(Integer.parseInt(record.get(12)));
                if (NumberUtils.isParsable(record.get(13))) builder.sp(Integer.parseInt(record.get(13)));
                if (NumberUtils.isParsable(record.get(14))) builder.ap(Integer.parseInt(record.get(14)));
                builder.apType(record.get(15));
                if (NumberUtils.isParsable(record.get(16))) builder.hwan(Byte.parseByte(record.get(16)));
                if (NumberUtils.isParsable(record.get(17))) builder.inventorySlots(Byte.parseByte(record.get(17)));
                if (NumberUtils.isParsable(record.get(18))) builder.itemRewardType(Byte.parseByte(record.get(18)));
            }
            return builder.build();
        } catch (Exception e) { return null; }
    }
}
