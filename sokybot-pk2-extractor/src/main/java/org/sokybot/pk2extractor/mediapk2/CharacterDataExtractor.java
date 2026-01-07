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
import org.sokybot.pk2extractor.dto.CharacterData;
import org.sokybot.pk2extractor.exception.Pk2MissedResourceException;

/**
 * Extracts enhanced character (NPC/Monster) data from characterdata.txt.
 * Pure extraction with streaming callbacks - no caching or persistence.
 */
public class CharacterDataExtractor implements IExtractor<CharacterData> {

    private static final String NAME = "Character Data";
    private static final String INDEX_FILE = "characterdata.txt";

    @Override
    public Class<CharacterData> getDtoClass() {
        return CharacterData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<CharacterData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }
            
            // Extract from characterdata.txt index file
            driver.findFirst(INDEX_FILE)
                .map(jmx -> Pk2ExtractorUtils.toString(jmx, StandardCharsets.UTF_16.name()))
                .map(Pk2ExtractorUtils::toLines)
                .orElseThrow(() -> new Pk2MissedResourceException(
                    "Could not find " + INDEX_FILE, INDEX_FILE))
                .flatMap(fileName -> driver.find("(?i)" + fileName).stream())
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 69 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    CharacterData dto = toCharacterData(record);
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
            
            // Also extract from teleportbuilding.txt (same structure)
            driver.find("(?i)teleportbuilding\\.txt").stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 69 && !r.get(0).startsWith("//"))
                .forEach(record -> {
                    CharacterData dto = toCharacterData(record);
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

    private CharacterData toCharacterData(CSVRecord record) {
        try {
            var builder = CharacterData.builder();
            
            // Field 1: RefID
            if (NumberUtils.isParsable(record.get(1))) {
                builder.refId(Integer.parseInt(record.get(1)));
            }
            
            // Field 2: CodeName
            builder.longId(record.get(2));
            
            // Field 5: Name
            builder.name(record.get(5));
            
            // Field 15: Rarity
            if (NumberUtils.isParsable(record.get(15))) {
                builder.rarity(Byte.parseByte(record.get(15)));
            }
            
            // Field 57: Level
            if (record.size() > 57 && NumberUtils.isParsable(record.get(57))) {
                builder.level(Integer.parseInt(record.get(57)));
            }
            
            // Field 59: HP
            if (record.size() > 59 && NumberUtils.isParsable(record.get(59))) {
                builder.maxHP(Integer.parseInt(record.get(59)));
            }
            
            // Field 60: MP
            if (record.size() > 60 && NumberUtils.isParsable(record.get(60))) {
                builder.maxMP(Integer.parseInt(record.get(60)));
            }
            
            // Field 61: Inventory Size
            if (record.size() > 61 && NumberUtils.isParsable(record.get(61))) {
                builder.inventorySize(Integer.parseInt(record.get(61)));
            }
            
            // Field 62-65: Store TIDs
            if (record.size() > 62) builder.canStoreTID1(!record.get(62).equals("0"));
            if (record.size() > 63) builder.canStoreTID2(!record.get(63).equals("0"));
            if (record.size() > 64) builder.canStoreTID3(!record.get(64).equals("0"));
            if (record.size() > 65) builder.canStoreTID4(!record.get(65).equals("0"));
            
            // Field 66: CanBeVehicle
            if (record.size() > 66 && NumberUtils.isParsable(record.get(66))) {
                builder.canBeVehicle(BooleanUtils.toBoolean(Byte.valueOf(record.get(66))));
            }
            
            // Field 67: CanControl
            if (record.size() > 67 && NumberUtils.isParsable(record.get(67))) {
                builder.canControl(BooleanUtils.toBoolean(Byte.valueOf(record.get(67))));
            }
            
            // Field 69: MaxPassenger
            if (record.size() > 69 && NumberUtils.isParsable(record.get(69))) {
                builder.maxPassenger(Integer.parseInt(record.get(69)));
            }
            
            // Derived flags
            String codeName = record.get(2);
            if (codeName != null) {
                if (codeName.equals("SN_MOB_GOD_PILLAR")) builder.isDimensionPillar(true);
                if (codeName.startsWith("STRUCTURE_SUMMON_FLOWER_")) builder.isSummonFlower(true);
                if (codeName.startsWith("MOB_EV")) builder.isEventMob(true);
            }
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
