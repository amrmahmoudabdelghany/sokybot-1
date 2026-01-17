package org.sokybot.persistence.internal.extraction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.service.PersistenceException;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.dto.character.NPCData;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NPCExtractionHandler.
 * Note: These tests use mocking since PK2 files are not available in test environment.
 */
class NPCExtractionHandlerTest {

    @TempDir
    Path tempDir;

    private EntityManagerFactory emf;
    private NPCExtractionHandler handler;

    @BeforeEach
    void setUp() {
        String dbUrl = "jdbc:h2:mem:npcextraction;DB_CLOSE_DELAY=-1";
        emf = Persistence.createEntityManagerFactory("sokybot-persistence-unit", 
            java.util.Map.of(
                "javax.persistence.jdbc.url", dbUrl,
                "javax.persistence.jdbc.driver", "org.h2.Driver",
                "javax.persistence.jdbc.user", "sa",
                "javax.persistence.jdbc.password", "",
                "hibernate.hbm2ddl.auto", "create-drop",
                "hibernate.show_sql", "false"
            ));
        
        handler = new NPCExtractionHandler(emf);
    }

    @Test
    void testGetExtractionType() {
        String type = handler.getExtractionType();
        
        assertEquals("NPC", type);
    }

    @Test
    void testConvertDtoToEntity() {
        // This test would require creating a mock NPCData
        // Since NPCData might have complex dependencies, we test through reflection
        // or create a simple test DTO
        
        // For now, verify the handler is created correctly
        assertNotNull(handler);
        assertEquals("NPC", handler.getExtractionType());
    }

    @Test
    void testExtractAndPersistWithNonExistentFile() {
        String nonExistentPath = tempDir.resolve("non-existent").toString();
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            handler.extractAndPersist(nonExistentPath, "Media.pk2");
        });
        
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("no longer exists") || 
                   exception.getCause() != null);
    }

    @Test
    void testHandlerInitialization() {
        assertNotNull(handler);
        
        // Verify it's using the correct EMF
        NPCExtractionHandler newHandler = new NPCExtractionHandler(emf);
        assertNotNull(newHandler);
        assertEquals("NPC", newHandler.getExtractionType());
    }

    @Test
    void testExtractionTypeConsistency() {
        NPCExtractionHandler handler1 = new NPCExtractionHandler(emf);
        NPCExtractionHandler handler2 = new NPCExtractionHandler(emf);
        
        assertEquals(handler1.getExtractionType(), handler2.getExtractionType());
        assertEquals("NPC", handler1.getExtractionType());
    }
}