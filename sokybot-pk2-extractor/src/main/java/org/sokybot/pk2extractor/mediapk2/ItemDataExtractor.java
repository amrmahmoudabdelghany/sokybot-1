package org.sokybot.pk2extractor.mediapk2;

import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.ItemData;
import org.sokybot.pk2extractor.dto.ItemTypeData;
import org.sokybot.pk2extractor.exception.Pk2MissedResourceException;

/**
 * Extracts detailed item data from pk2 files.
 * Pure extraction with streaming callbacks - no caching or persistence.
 */
public class ItemDataExtractor implements IExtractor<ItemData> {

    private static final String NAME = "Item Data";
    private static final String FILE_PATTERN = "(?i)itemdata\\w*\\.txt";

    @Override
    public Class<ItemData> getDtoClass() {
        return ItemData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<ItemData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            // Find all itemdata files and process them
            driver.find(FILE_PATTERN).stream()
                .flatMap(jmx -> Pk2ExtractorUtils.toCSVRecordStream(jmx, StandardCharsets.UTF_16))
                .filter(r -> r.size() > 57 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    ItemData dto = toItemData(record);
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

    private ItemData toItemData(CSVRecord record) {
        try {
            var builder = ItemData.builder();
            
            // Basic fields
            if (NumberUtils.isParsable(record.get(1))) {
                builder.refId(Integer.parseInt(record.get(1)));
            }
            builder.longId(record.get(2));
            builder.name(record.get(5));
            
            if (NumberUtils.isParsable(record.get(7))) {
                builder.isMallItem(BooleanUtils.toBoolean(Byte.valueOf(record.get(7))));
            }
            
            // Type parsing
            if (record.size() > 14) {
                int type1 = safeParseInt(record.get(9));
                int type2 = safeParseInt(record.get(10));
                int type3 = safeParseInt(record.get(11));
                int type4 = safeParseInt(record.get(12));
                builder.itemType(ItemTypeData.of(type1, type2, type3, type4));
            }
            
            // Rarity
            if (NumberUtils.isParsable(record.get(15))) {
                builder.rarity(Byte.parseByte(record.get(15)));
            }
            
            // Flags
            if (NumberUtils.isParsable(record.get(16))) {
                builder.canTrade(BooleanUtils.toBoolean(Byte.valueOf(record.get(16))));
            }
            if (NumberUtils.isParsable(record.get(17))) {
                builder.canSell(BooleanUtils.toBoolean(Byte.valueOf(record.get(17))));
            }
            if (NumberUtils.isParsable(record.get(18))) {
                builder.canBuy(BooleanUtils.toBoolean(Byte.valueOf(record.get(18))));
            }
            if (NumberUtils.isParsable(record.get(20))) {
                builder.canDrop(BooleanUtils.toBoolean(Byte.valueOf(record.get(20))));
            }
            if (NumberUtils.isParsable(record.get(22))) {
                builder.canRepair(BooleanUtils.toBoolean(Byte.valueOf(record.get(22))));
            }
            if (NumberUtils.isParsable(record.get(24))) {
                builder.canUse(BooleanUtils.toBoolean(Byte.valueOf(record.get(24))));
            }
            
            // Price
            if (NumberUtils.isParsable(record.get(26))) {
                builder.price(Integer.parseInt(record.get(26)));
            }
            
            // Sell price
            if (record.size() > 31 && NumberUtils.isParsable(record.get(31))) {
                builder.sellPrice(Integer.parseInt(record.get(31)));
            }
            
            // Icon
            if (record.size() > 54) {
                builder.iconPath(record.get(54));
            }
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
    
    private int safeParseInt(String str) {
        return NumberUtils.isParsable(str) ? Integer.parseInt(str) : 0;
    }
}
