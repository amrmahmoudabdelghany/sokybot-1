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
import org.sokybot.app.builders.pk2extractor.dto.CharacterData;
import org.sokybot.app.builders.pk2extractor.dto.GenderData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts enhanced character (NPC/Monster) data.
 * Reference: RSBot RefObjChar
 */
@Slf4j
@Component(service = IExtractor.class)
public class CharacterDataExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public CharacterDataExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting detailed character data (CharacterData) from media.pk2");
        
        // Primary CharacterData (from file list)
        List<CharacterData> chars = driver.findFirst("characterdata.txt")
                .map(jmx -> Pk2ExtractorUtils.toString(jmx, StandardCharsets.UTF_16.name()))
                .map(Pk2ExtractorUtils::toLines)
                .orElse(java.util.stream.Stream.empty())
                .flatMap((fileName) -> driver.find("(?i)" + fileName).stream())
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 69 && !r.get(0).startsWith("//"))
                .map(this::toCharacterData)
                .collect(Collectors.toList());
//                .distinct()
//                .collect(Collectors.toList());

        // Secondary: TeleportBuilding.txt (Direct file, same structure)
        List<CharacterData> buildings = driver.find("(?i)teleportbuilding\\.txt")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 69 && !r.get(0).startsWith("//"))
                .map(this::toCharacterData)
                .collect(Collectors.toList());
        
        chars.addAll(buildings);
        
        // Deduplicate and process
        chars = chars.stream().distinct().collect(Collectors.toList());
        
        log.info("Extracted {} detailed character entries", chars.size());
        
        chars.forEach(c -> cache.put("CHAR_DATA_" + c.getLongId(), c));
    }
    
    private CharacterData toCharacterData(CSVRecord record) {
        var builder = CharacterData.builder();
        
        try {
            // Field 1: RefID
            if (NumberUtils.isParsable(record.get(1))) builder.refId(Integer.parseInt(record.get(1)));
            
            // Field 2: CodeName
            builder.longId(record.get(2));
            
            // Field 5: Name (Localized or raw)
            String nameKey = record.get(5);
            // String localizedName = cache.get(nameKey, String.class);
            // builder.name(localizedName != null ? localizedName : nameKey);
            builder.name(nameKey); // Placeholder
            
            // Field 15: Rarity (Common)
            if (NumberUtils.isParsable(record.get(15))) builder.rarity(Byte.parseByte(record.get(15)));
            
            // Field 57: Level
            if (record.size() > 57 && NumberUtils.isParsable(record.get(57))) builder.level(Integer.parseInt(record.get(57)));
            
            // Field 58: Gender
            if (record.size() > 58 && NumberUtils.isParsable(record.get(58))) {
                // Assuming standard gender bytes (0=Female, 1=Male, etc. or specific Enum)
                // GenderData enum: Female(0), Male(1), Unisex(2)? 
                // Let's assume byte matching GenderData
                // builder.gender(GenderData.values()[Byte.parseByte(record.get(58))]); // Risky if out of bounds
            }
            
            // Field 59: HP
            if (record.size() > 59 && NumberUtils.isParsable(record.get(59))) builder.maxHP(Integer.parseInt(record.get(59)));
            
            // Field 60: MP
            if (record.size() > 60 && NumberUtils.isParsable(record.get(60))) builder.maxMP(Integer.parseInt(record.get(60)));
            
            // Field 61: Inventory Size
            if (record.size() > 61 && NumberUtils.isParsable(record.get(61))) builder.inventorySize(Integer.parseInt(record.get(61)));
            
            // Field 62-65: Store TIDs
            if (record.size() > 62) builder.canStoreTID1(!record.get(62).equals("0"));
            if (record.size() > 63) builder.canStoreTID2(!record.get(63).equals("0"));
            if (record.size() > 64) builder.canStoreTID3(!record.get(64).equals("0"));
            if (record.size() > 65) builder.canStoreTID4(!record.get(65).equals("0"));
            
            // Field 66: CanBeVehicle
            if (record.size() > 66 && NumberUtils.isParsable(record.get(66))) builder.canBeVehicle(BooleanUtils.toBoolean(Byte.valueOf(record.get(66))));
            
            // Field 67: CanControl
            if (record.size() > 67 && NumberUtils.isParsable(record.get(67))) builder.canControl(BooleanUtils.toBoolean(Byte.valueOf(record.get(67))));
            
            // Field 69: MaxPassenger
            if (record.size() > 69 && NumberUtils.isParsable(record.get(69))) builder.maxPassenger(Integer.parseInt(record.get(69)));

            // Derived flags
            String codeName = record.get(2);
            if(codeName != null) {
                if (codeName.equals("SN_MOB_GOD_PILLAR")) builder.isDimensionPillar(true);
                if (codeName.startsWith("STRUCTURE_SUMMON_FLOWER_")) builder.isSummonFlower(true);
                if (codeName.startsWith("MOB_EV")) builder.isEventMob(true);
            }

        } catch (Exception e) {
            log.warn("Failed to parse detailed character record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
