package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.RefShopTabData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts shop tab definitions from refshoptab.txt.
 * Reference: RSBot RefShopTab
 */
@Slf4j
@Component(service = IExtractor.class)
public class RefShopTabExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public RefShopTabExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting shop tab data from media.pk2 file");
        
        List<RefShopTabData> tabs = driver.find("(?i)refshoptab.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 5 && !r.get(0).startsWith("//"))
                .map(this::toRefShopTabData)
                .filter(t -> t.getId() > 0)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} shop tab entries", tabs.size());
        
        tabs.forEach(tab -> cache.put("SHOP_TAB_" + tab.getCodeName(), tab));
    }
    
    private RefShopTabData toRefShopTabData(CSVRecord record) {
        var builder = RefShopTabData.builder();
        
        try {
            // Field 0: Service
            if (NumberUtils.isParsable(record.get(0))) builder.service(Integer.parseInt(record.get(0)));
            
            // Field 1: Country
            if (NumberUtils.isParsable(record.get(1))) builder.country(Integer.parseInt(record.get(1)));
            
            // Field 2: ID
            if (NumberUtils.isParsable(record.get(2))) builder.id(Integer.parseInt(record.get(2)));
            
            // Field 3: CodeName
            builder.codeName(record.get(3));
            
            // Field 4: Tab Group Code
            builder.refTabGroupCodeName(record.get(4));
            
            // Field 5: StrID (Localized Name)
            builder.strID128_Tab(record.get(5));
            
        } catch (Exception e) {
            log.warn("Failed to parse shop tab record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
