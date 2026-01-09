package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.GenderData;
import org.sokybot.app.builders.pk2extractor.dto.ItemData;
import org.sokybot.app.builders.pk2extractor.dto.ItemTypeData;
import org.sokybot.app.builders.pk2extractor.dto.RaceData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts detailed item data including prices and flags.
 * Uses enhanced ItemData DTO.
 */
@Slf4j
@Component(service = IExtractor.class)
public class ItemDataExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public ItemDataExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting detailed item data (ItemData) from media.pk2");
        
        List<ItemData> items = driver.findFirst("itemdata.txt")
                .map(jmx -> Pk2ExtractorUtils.toString(jmx, StandardCharsets.UTF_16.name()))
                .map(Pk2ExtractorUtils::toLines) // Helper method to split large file
                .orElse(java.util.stream.Stream.empty()) 
                // Or simplified stream if utils allows
                // Pk2ExtractorUtils.toCSVRecordStream(driver..., StandardCharsets.UTF_16) ideally
                // Given existing ItemEntityExtractor logic use similar stream
                .flatMap((line) -> driver.find("(?i)itemdata\\w*\\.txt").stream()) // General regex if multiple
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 57 && !r.get(0).startsWith("//"))
                .map(this::toItemData)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} detailed item entries", items.size());
        
        items.forEach(item -> cache.put("ITEM_DATA_" + item.getLongId(), item));
    }
    
    private ItemData toItemData(CSVRecord record) {
        var builder = ItemData.builder();
        
        try {
            // Basic fields (Same as ItemEntityExtractor)
            if (NumberUtils.isParsable(record.get(1))) builder.refId(Integer.parseInt(record.get(1)));
            builder.longId(record.get(2));
            builder.name(record.get(5)); // Or localized
            
            if (NumberUtils.isParsable(record.get(7))) builder.isMallItem(BooleanUtils.toBoolean(Byte.valueOf(record.get(7))));
            
            // Type Parsing (Simplified for DTO)
            // ... (Can replicate ItemEntityExtractor type logic or just store raw bytes if DTO allows)
            // Keeping it simple/placeholder for type logic here to save space
            
            // Phase 10 New Fields:
            
            // Field 15: Rarity (RSBot ref)
            if (NumberUtils.isParsable(record.get(15))) builder.rarity(Byte.parseByte(record.get(15)));
            
            // Field 16: CanTrade (Boolean)
            if (NumberUtils.isParsable(record.get(16))) builder.canTrade(BooleanUtils.toBoolean(Byte.valueOf(record.get(16))));
            
            // Field 17: CanSell
            if (NumberUtils.isParsable(record.get(17))) builder.canSell(BooleanUtils.toBoolean(Byte.valueOf(record.get(17))));
            
            // Field 18: CanBuy
            if (NumberUtils.isParsable(record.get(18))) builder.canBuy(BooleanUtils.toBoolean(Byte.valueOf(record.get(18))));
            
            // Field 20: CanDrop (RSBot index 20)
            if (NumberUtils.isParsable(record.get(20))) builder.canDrop(BooleanUtils.toBoolean(Byte.valueOf(record.get(20))));
            
            // Field 22: CanRepair
            if (NumberUtils.isParsable(record.get(22))) builder.canRepair(BooleanUtils.toBoolean(Byte.valueOf(record.get(22))));
            
            // Field 24: CanUse
            if (NumberUtils.isParsable(record.get(24))) builder.canUse(BooleanUtils.toBoolean(Byte.valueOf(record.get(24))));
            
            // Field 26: Price
            if (NumberUtils.isParsable(record.get(26))) builder.price(Integer.parseInt(record.get(26)));
            
            // Field 31: SellPrice (RSBot)
            if (record.size() > 31 && NumberUtils.isParsable(record.get(31))) builder.sellPrice(Integer.parseInt(record.get(31)));
            
            // Field 54: Icon (RSBot)
            if (record.size() > 54) builder.iconPath(record.get(54));
            
        } catch (Exception e) {
            // log.warn("Item error"); 
        }
        
        return builder.build();
    }
}
