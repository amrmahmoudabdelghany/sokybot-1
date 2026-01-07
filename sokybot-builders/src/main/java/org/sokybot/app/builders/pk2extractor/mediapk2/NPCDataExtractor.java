package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.NPCData;
import org.sokybot.app.builders.pk2extractor.dto.NPCTypeData;
import org.sokybot.app.builders.pk2extractor.exception.Pk2MissedResourceException;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts NPC data from pk2 files and outputs NPCData DTOs.
 * Pure extraction - no persistence logic.
 */
@Slf4j
@Component(service = IExtractor.class)
public class NPCDataExtractor implements IExtractor {

    private Cache cache;

    @Reference
    public NPCDataExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }

    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting NPC data from media.pk2 file");

        List<NPCData> list = driver.findFirst("characterdata.txt")
                .map(jmx -> Pk2ExtractorUtils.toString(jmx, StandardCharsets.UTF_16.name()))
                .map(Pk2ExtractorUtils::toLines)
                .orElseThrow(() -> new Pk2MissedResourceException("Could not find characterdata.txt file",
                        "characterdata.txt"))
                .flatMap((npcFileName) -> driver.find("(?i)" + npcFileName).stream())
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .map(this::toNPCData)
                .peek((npc) -> cache.put(npc.getLongId(), npc.getRefId()))
                .collect(Collectors.toList());

        // Cache all NPCData for downstream consumers
        list.forEach(npc -> cache.put("NPC_DATA_" + npc.getRefId(), npc));
        
        log.info("Extracted {} NPC entries", list.size());
    }

    private NPCData toNPCData(CSVRecord record) {
        String field = record.get(1);
        int refId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;

        field = record.get(5);
        String name = this.cache.get(field, String.class);
        name = (name == null) ? field : name;

        field = record.get(57);
        int lvl = NumberUtils.isParsable(field) ? Integer.parseInt(field) : 0;

        field = record.get(59);
        int hp = NumberUtils.isParsable(field) ? Integer.parseInt(field) : 0;

        String typeFlag = record.get(10) + record.get(11) + record.get(12) + record.get(14) + record.get(15);
        int typeValue = NumberUtils.isParsable(typeFlag) ? Integer.parseInt(typeFlag) : -1;

        return NPCData.builder()
                .refId(refId)
                .longId(record.get(2))
                .name(name)
                .level(lvl)
                .HP(hp)
                .type(NPCTypeData.of(typeValue))
                .build();
    }
}
