package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.TeleportData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts Teleport data from pk2 files and outputs TeleportData DTOs.
 * Pure extraction - no persistence logic.
 */
@Slf4j
@Component(service = IExtractor.class)
public class TeleportDataExtractor implements IExtractor {

    private Cache cache;
    private Map<Integer, List<Integer>> links = new HashMap<>();

    @Reference
    public TeleportDataExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }

    private void extractLinks(IPk2Driver driver) {
        log.info("Extracting links from teleportlink.txt");
        driver.find("teleportlink.txt").forEach((jmx) -> {
            Pk2ExtractorUtils.toCSVRecordStream(jmx).forEach((record) -> {
                String field = record.get(1);
                int id = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
                
                field = record.get(2);
                int link = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
                
                List<Integer> linkArr = links.get(id);
                if (linkArr == null) {
                    linkArr = new ArrayList<>();
                    links.put(id, linkArr);
                }
                linkArr.add(link);
            });
        });
    }

    @Override
    public void extract(IPk2Driver driver) {
        extractLinks(driver);

        log.info("Extracting teleport data from teleportdata.txt");
        
        driver.find("teleportdata.txt").forEach((teleportdata) -> {
            List<TeleportData> teleports = Pk2ExtractorUtils.toCSVRecordStream(teleportdata)
                    .map((record) -> {
                        String field = record.get(1);
                        int refId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
                        
                        String longId = record.get(2);
                        
                        field = record.get(3);
                        int buildId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
                        
                        field = record.get(4);
                        String name = this.cache.get(field, String.class);
                        name = (name == null) ? field : name;

                        return TeleportData.builder()
                                .refId(refId)
                                .longId(longId)
                                .portalId(buildId)
                                .name(name)
                                .links(links.getOrDefault(refId, new ArrayList<>()))
                                .build();
                    })
                    .collect(Collectors.toList());

            // Cache all TeleportData for downstream consumers
            teleports.forEach(teleport -> cache.put("TELEPORT_DATA_" + teleport.getRefId(), teleport));

            log.info("Extracted {} teleport entries", teleports.size());
        });
    }
}
