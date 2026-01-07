package org.sokybot.app.builders.pk2extractor.datapk2;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;

// Import datapk2 reactive extractors
import org.sokybot.pk2extractor.datapk2.ObjectInfoExtractor;
import org.sokybot.pk2extractor.datapk2.NavMeshExtractor;

import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates extraction from Data.pk2 using reactive extractors
 * from sokybot-pk2-extractor with caching/persistence support.
 */
@Component(service = IExtractor.class)
@Slf4j
public class ReactiveDataPk2Extractor implements IExtractor {

    private final List<org.sokybot.pk2extractor.IExtractor<?>> extractors = new ArrayList<>();

    public ReactiveDataPk2Extractor() {
        // Register datapk2 extractors
        extractors.add(new ObjectInfoExtractor());
        extractors.add(new NavMeshExtractor());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void extract(IPk2Driver driver) {
        log.info("Starting reactive extraction from Data.pk2 ({} extractors)", extractors.size());
        
        long startTime = System.currentTimeMillis();
        int totalExtracted = 0;
        
        for (org.sokybot.pk2extractor.IExtractor<?> extractor : extractors) {
            try {
                log.info("Running extractor: {}", extractor.getName());
                
                int[] count = {0};
                ExtractionListener<Object> listener = new ExtractionListener<Object>() {
                    @Override
                    public void onExtracted(Object dto) {
                        count[0]++;
                        // TODO: Implement persistence here
                        // repository.save(dto);
                    }
                    
                    @Override
                    public void onComplete(int totalCount) {
                        log.info("Extractor {} completed: {} items", extractor.getName(), totalCount);
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        log.error("Extractor {} failed: {}", extractor.getName(), error.getMessage());
                    }
                };
                
                ExtractionProgressListener progressListener = new ExtractionProgressListener() {
                    @Override
                    public void onStart(String extractorName, int totalItems) {}
                    
                    @Override
                    public void onProgress(String extractorName, int current, int total, String itemId) {}
                    
                    @Override
                    public void onComplete(String extractorName, int totalItems, long durationMs) {
                        log.debug("{} completed in {}ms", extractorName, durationMs);
                    }
                    
                    @Override
                    public void onError(String extractorName, Exception error) {
                        log.error("{} error: {}", extractorName, error.getMessage());
                    }
                };
                
                ((org.sokybot.pk2extractor.IExtractor<Object>) extractor)
                    .extract(driver, listener, progressListener);
                
                totalExtracted += count[0];
                
            } catch (Exception e) {
                log.error("Failed to run extractor {}: {}", extractor.getName(), e.getMessage());
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Reactive Data.pk2 extraction completed: {} total items in {}ms", totalExtracted, duration);
    }
}
