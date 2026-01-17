package org.sokybot.persistence.internal.extraction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.service.PersistenceException;
import org.sokybot.pk2extractor.dto.character.NPCData;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CachedExtractionDecorator.
 */
class CachedExtractionDecoratorTest {

    private IPk2ExtractionHandler<NPCData, NPCEntity> mockHandler;
    private CachedExtractionDecorator<NPCData, NPCEntity> decorator;
    private java.util.function.Supplier<Boolean> hasExtractedSupplier;
    private String gamePath = "/test/game/path";
    private String pk2FileName = "Media.pk2";

    @BeforeEach
    void setUp() {
        mockHandler = mock(IPk2ExtractionHandler.class);
        when(mockHandler.getExtractionType()).thenReturn("NPC");
    }

    @Test
    void testSkipExtractionWhenAlreadyExtracted() throws PersistenceException {
        hasExtractedSupplier = () -> true; // Already extracted
        decorator = new CachedExtractionDecorator<>(mockHandler, hasExtractedSupplier);
        
        decorator.extractAndPersist(gamePath, pk2FileName);
        
        // Should not call the underlying handler
        verify(mockHandler, never()).extractAndPersist(gamePath, pk2FileName);
    }

    @Test
    void testExtractWhenNotExtracted() throws PersistenceException {
        hasExtractedSupplier = () -> false; // Not extracted
        decorator = new CachedExtractionDecorator<>(mockHandler, hasExtractedSupplier);
        
        doNothing().when(mockHandler).extractAndPersist(gamePath, pk2FileName);
        
        decorator.extractAndPersist(gamePath, pk2FileName);
        
        // Should call the underlying handler
        verify(mockHandler, times(1)).extractAndPersist(gamePath, pk2FileName);
    }

    @Test
    void testSupplierCalledOnEachExtraction() throws PersistenceException {
        hasExtractedSupplier = mock(java.util.function.Supplier.class);
        when(hasExtractedSupplier.get()).thenReturn(false, true); // First time: not extracted, second time: extracted
        
        decorator = new CachedExtractionDecorator<>(mockHandler, hasExtractedSupplier);
        
        doNothing().when(mockHandler).extractAndPersist(gamePath, pk2FileName);
        
        // First call - should extract
        decorator.extractAndPersist(gamePath, pk2FileName);
        
        // Second call - should skip
        decorator.extractAndPersist(gamePath, pk2FileName);
        
        verify(hasExtractedSupplier, times(2)).get();
        verify(mockHandler, times(1)).extractAndPersist(gamePath, pk2FileName);
    }

    @Test
    void testGetExtractionType() {
        hasExtractedSupplier = () -> false;
        decorator = new CachedExtractionDecorator<>(mockHandler, hasExtractedSupplier);
        
        String type = decorator.getExtractionType();
        
        assertEquals("NPC", type);
        verify(mockHandler).getExtractionType();
    }

    @Test
    void testExceptionPropagationWhenNotCached() throws PersistenceException {
        hasExtractedSupplier = () -> false;
        decorator = new CachedExtractionDecorator<>(mockHandler, hasExtractedSupplier);
        
        PersistenceException exception = new PersistenceException("Extraction failed");
        doThrow(exception).when(mockHandler).extractAndPersist(gamePath, pk2FileName);
        
        PersistenceException thrown = assertThrows(PersistenceException.class, () -> {
            decorator.extractAndPersist(gamePath, pk2FileName);
        });
        
        assertEquals(exception, thrown);
    }

    @Test
    void testSupplierReturnsNull() throws PersistenceException {
        // Null should be treated as false (not extracted)
        hasExtractedSupplier = () -> null;
        decorator = new CachedExtractionDecorator<>(mockHandler, hasExtractedSupplier);
        
        doNothing().when(mockHandler).extractAndPersist(gamePath, pk2FileName);
        
        decorator.extractAndPersist(gamePath, pk2FileName);
        
        // Should proceed with extraction (null != true)
        verify(mockHandler, times(1)).extractAndPersist(gamePath, pk2FileName);
    }
}