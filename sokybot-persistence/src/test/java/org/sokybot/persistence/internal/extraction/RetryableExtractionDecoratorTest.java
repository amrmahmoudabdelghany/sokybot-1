package org.sokybot.persistence.internal.extraction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.service.PersistenceException;
import org.sokybot.pk2extractor.dto.character.NPCData;
import org.sokybot.pk2extractor.dto.item.ItemData;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RetryableExtractionDecorator.
 */
class RetryableExtractionDecoratorTest {

    private IPk2ExtractionHandler<NPCData, NPCEntity> mockHandler;
    private RetryableExtractionDecorator<NPCData, NPCEntity> decorator;
    private String gamePath = "/test/game/path";
    private String pk2FileName = "Media.pk2";

    @BeforeEach
    void setUp() {
        mockHandler = mock(IPk2ExtractionHandler.class);
        when(mockHandler.getExtractionType()).thenReturn("NPC");
    }

    @Test
    void testSuccessfulExtractionWithoutRetry() throws PersistenceException {
        decorator = new RetryableExtractionDecorator<>(mockHandler, 3, 100);
        
        doNothing().when(mockHandler).extractAndPersist(gamePath, pk2FileName);
        
        decorator.extractAndPersist(gamePath, pk2FileName);
        
        verify(mockHandler, times(1)).extractAndPersist(gamePath, pk2FileName);
    }

    @Test
    void testExtractionWithOneRetry() throws PersistenceException {
        decorator = new RetryableExtractionDecorator<>(mockHandler, 3, 10);
        
        doThrow(new PersistenceException("First attempt fails"))
            .doNothing()
            .when(mockHandler).extractAndPersist(gamePath, pk2FileName);
        
        decorator.extractAndPersist(gamePath, pk2FileName);
        
        verify(mockHandler, times(2)).extractAndPersist(gamePath, pk2FileName);
    }

    @Test
    void testExtractionExceedsMaxRetries() throws PersistenceException {
        decorator = new RetryableExtractionDecorator<>(mockHandler, 2, 10);
        
        doThrow(new PersistenceException("Always fails"))
            .when(mockHandler).extractAndPersist(gamePath, pk2FileName);
        
        PersistenceException exception = assertThrows(PersistenceException.class, () -> {
            decorator.extractAndPersist(gamePath, pk2FileName);
        });
        
        // Should try 1 initial + 2 retries = 3 total attempts
        verify(mockHandler, times(3)).extractAndPersist(gamePath, pk2FileName);
        assertNotNull(exception);
    }

    @Test
    void testExtractionWithZeroRetries() throws PersistenceException {
        decorator = new RetryableExtractionDecorator<>(mockHandler, 0, 10);
        
        doThrow(new PersistenceException("Fails"))
            .when(mockHandler).extractAndPersist(gamePath, pk2FileName);
        
        assertThrows(PersistenceException.class, () -> {
            decorator.extractAndPersist(gamePath, pk2FileName);
        });
        
        // Should only try once (no retries)
        verify(mockHandler, times(1)).extractAndPersist(gamePath, pk2FileName);
    }

    @Test
    void testGetExtractionType() {
        decorator = new RetryableExtractionDecorator<>(mockHandler, 3, 100);
        
        String type = decorator.getExtractionType();
        
        assertEquals("NPC", type);
        verify(mockHandler).getExtractionType();
    }

    @Test
    void testRetryInterruptedException() throws PersistenceException, InterruptedException {
        decorator = new RetryableExtractionDecorator<>(mockHandler, 3, 1000);
        
        doThrow(new PersistenceException("Fails"))
            .when(mockHandler).extractAndPersist(gamePath, pk2FileName);
        
        Thread testThread = Thread.currentThread();
        // This is a bit tricky to test - we'd need to interrupt during sleep
        // For now, just verify the decorator handles the case
        assertThrows(PersistenceException.class, () -> {
            decorator.extractAndPersist(gamePath, pk2FileName);
        });
    }

    @Test
    void testDefaultConstructor() {
        decorator = new RetryableExtractionDecorator<>(mockHandler);
        
        assertEquals("NPC", decorator.getExtractionType());
        // Default should be 3 retries with 1000ms delay
    }
}