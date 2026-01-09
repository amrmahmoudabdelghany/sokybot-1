package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.dto.RefTextData;
import org.sokybot.pk2.IPk2Driver;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts localization text data from textdata_*.txt (e.g., textdata_object.txt).
 * Reference: RSBot RefText
 */
@Slf4j
@Component(service = IExtractor.class)
public class RefTextExtractor implements IExtractor {
    
    private Cache cache;
    
    @Reference
    public RefTextExtractor(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }
    
    @Override
    public void extract(IPk2Driver driver) {
        log.info("Extracting localization text data from media.pk2 file");
        
        // Matches textdata_*.txt files (object, equip, etc.)
        List<RefTextData> textData = driver.find("(?i)textdata_.*\\.txt$")
                .stream()
                .flatMap(Pk2ExtractorUtils::toCSVRecordStream)
                .filter(r -> r.size() > 2 && !r.get(0).startsWith("//"))
                // Filter specifically for valid SN_ keys if possible, but general extraction is safer
                .map(this::toRefTextData)
                .filter(d -> d.getNameStrId() != null && !d.getNameStrId().isBlank())
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Extracted {} localization text entries", textData.size());
        
        textData.forEach(text -> cache.put("TEXT_" + text.getNameStrId(), text));
    }
    
    private RefTextData toRefTextData(CSVRecord record) {
        var builder = RefTextData.builder();
        
        try {
            // Field 0: Service
            String field = record.get(0);
            if (NumberUtils.isParsable(field)) {
                builder.service(Integer.parseInt(field));
            }
            
            // Note: Indices for TextData vary by region/version.
            // Standard: 0=Service, 1=Key (old) or 2=Key (new), 8+=Lang Data
            
            // Try to identify Key index. Usually starts with SN_
            int keyIndex = 1;
            // Simple heuristic: check if col 1 starts with SN_ or is a number
            if (record.size() > 2 && !record.get(1).startsWith("SN_") && record.get(2).startsWith("SN_")) {
                keyIndex = 2;
            }
            
            builder.nameStrId(record.get(keyIndex));
            
            // Data index often depends on language. We'll grab the last filled column or a specific offset
            // RSBot uses offset 8 or 9 or 12 depending on client.
            // We'll take the LAST column as a fallback or the one after key
            int dataIndex = record.size() - 1;
             
            // Avoid empty last lines
            while (dataIndex > keyIndex && (record.get(dataIndex) == null || record.get(dataIndex).isBlank())) {
                dataIndex--;
            }
            
            if (dataIndex > keyIndex) {
                builder.data(record.get(dataIndex));
            } else {
                 // Fallback if no data found
                 builder.data(record.get(keyIndex)); // Use key as fallback
            }

        } catch (Exception e) {
            log.warn("Failed to parse text data record: {}", e.getMessage());
        }
        
        return builder.build();
    }
}
