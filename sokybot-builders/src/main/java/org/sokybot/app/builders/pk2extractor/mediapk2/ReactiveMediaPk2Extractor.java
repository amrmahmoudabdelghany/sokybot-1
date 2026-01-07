package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;

// Import all reactive extractors
import org.sokybot.pk2extractor.mediapk2.*;

import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates extraction from Media.pk2 using reactive extractors
 * from sokybot-pk2-extractor with caching/persistence support.
 */
@Component(service = IExtractor.class)
@Slf4j
public class ReactiveMediaPk2Extractor implements IExtractor {

    // All reactive extractors will be instantiated here
    private final List<org.sokybot.pk2extractor.IExtractor<?>> extractors = new ArrayList<>();

    public ReactiveMediaPk2Extractor() {
        // Register all mediapk2 extractors
        extractors.add(new LvlDataExtractor());
        extractors.add(new ItemDataExtractor());
        extractors.add(new NPCDataExtractor());
        extractors.add(new CharacterDataExtractor());
        extractors.add(new SkillDataExtractor());
        extractors.add(new MasteryDataExtractor());
        extractors.add(new SkillMasteryExtractor());
        extractors.add(new PortalDataExtractor());
        extractors.add(new TeleportDataExtractor());
        extractors.add(new TeleportLinkExtractor());
        extractors.add(new RegionDataExtractor());
        extractors.add(new OptionalTeleportExtractor());
        extractors.add(new QuestDataExtractor());
        extractors.add(new QuestRewardExtractor());
        extractors.add(new QuestRewardItemExtractor());
        extractors.add(new EventRewardExtractor());
        extractors.add(new MagicOptionExtractor());
        extractors.add(new MagicOptionAssignmentExtractor());
        extractors.add(new PackageItemExtractor());
        extractors.add(new ShopGroupExtractor());
        extractors.add(new RefShopTabExtractor());
        extractors.add(new RefMappingShopGroupExtractor());
        extractors.add(new RefTextExtractor());
        extractors.add(new FullLevelDataExtractor());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void extract(IPk2Driver driver) {
        log.info("Starting reactive extraction from Media.pk2 ({} extractors)", extractors.size());
        
        long startTime = System.currentTimeMillis();
        int totalExtracted = 0;
        
        for (org.sokybot.pk2extractor.IExtractor<?> extractor : extractors) {
            try {
                log.info("Running extractor: {}", extractor.getName());
                
                // Create a counting listener
                int[] count = {0};
                ExtractionListener<Object> listener = new ExtractionListener<Object>() {
                    @Override
                    public void onExtracted(Object dto) {
                        count[0]++;
                        // TODO: Implement caching/persistence here
                        // cache.put(dto.getClass().getSimpleName() + "_" + count[0], dto);
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
                
                // Progress listener for logging
                ExtractionProgressListener progressListener = new ExtractionProgressListener() {
                    @Override
                    public void onStart(String extractorName, int totalItems) {
                        // log.debug("Starting {}", extractorName);
                    }
                    
                    @Override
                    public void onProgress(String extractorName, int current, int total, String itemId) {
                        // log.trace("Progress {}: {}", extractorName, current);
                    }
                    
                    @Override
                    public void onComplete(String extractorName, int totalItems, long durationMs) {
                        log.debug("{} completed in {}ms", extractorName, durationMs);
                    }
                    
                    @Override
                    public void onError(String extractorName, Exception error) {
                        log.error("{} error: {}", extractorName, error.getMessage());
                    }
                };
                
                // Run the extractor with raw cast
                ((org.sokybot.pk2extractor.IExtractor<Object>) extractor)
                    .extract(driver, listener, progressListener);
                
                totalExtracted += count[0];
                
            } catch (Exception e) {
                log.error("Failed to run extractor {}: {}", extractor.getName(), e.getMessage());
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Reactive Media.pk2 extraction completed: {} total items in {}ms", totalExtracted, duration);
    }
}
