package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.PortalData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts Portal data from pk2 files and outputs PortalData DTOs.
 * Pure extraction - no persistence logic.
 */
@Slf4j
@Component(service = IExtractor.class)
public class PortalDataExtractor implements IExtractor {

    private Cache cache;

    @Reference
    public PortalDataExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }

    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting portal data from teleportbuilding.txt");

        driver.find("teleportbuilding.txt").forEach((teleportbuilding) -> {
            List<PortalData> portals = Pk2ExtractorUtils.toCSVRecordStream(teleportbuilding)
                    .map((record) -> {
                        String field = record.get(1);
                        int refId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
                        String longId = record.get(2);
                        
                        field = record.get(5);
                        String name = this.cache.get(field, String.class);
                        name = (name == null) ? field : name;

                        return PortalData.builder()
                                .refId(refId)
                                .longId(longId)
                                .name(name)
                                .build();
                    })
                    .collect(Collectors.toList());

            // Cache all PortalData for downstream consumers
            portals.forEach(portal -> cache.put("PORTAL_DATA_" + portal.getRefId(), portal));

            log.info("Extracted {} portal entries", portals.size());
        });
    }
}
