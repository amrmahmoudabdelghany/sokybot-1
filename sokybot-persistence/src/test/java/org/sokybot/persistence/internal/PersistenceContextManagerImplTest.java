package org.sokybot.persistence.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.persistence.EntityManagerFactory;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PersistenceContextManagerImpl.
 */
class PersistenceContextManagerImplTest {

    @TempDir
    Path tempDir;

    private PersistenceContextManagerImpl manager;

    @BeforeEach
    void setUp() {
        System.setProperty("sokybot.db.path", tempDir.toString());
        manager = new PersistenceContextManagerImpl();
        manager.activate(null);
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.deactivate();
        }
    }

    @Test
    void testGetEntityManagerFactory() {
        String gamePath = "/path/to/game";
        
        EntityManagerFactory emf1 = manager.getEntityManagerFactory(gamePath);
        EntityManagerFactory emf2 = manager.getEntityManagerFactory(gamePath);
        
        assertNotNull(emf1);
        assertNotNull(emf2);
        assertSame(emf1, emf2); // Should return same instance (cached)
        assertTrue(emf1.isOpen());
    }

    @Test
    void testGetEntityManagerFactoryDifferentGames() {
        String gamePath1 = "/path/to/game1";
        String gamePath2 = "/path/to/game2";
        
        EntityManagerFactory emf1 = manager.getEntityManagerFactory(gamePath1);
        EntityManagerFactory emf2 = manager.getEntityManagerFactory(gamePath2);
        
        assertNotNull(emf1);
        assertNotNull(emf2);
        assertNotSame(emf1, emf2); // Different games should have different EMFs
        assertTrue(emf1.isOpen());
        assertTrue(emf2.isOpen());
    }

    @Test
    void testCloseEntityManagerFactory() {
        String gamePath = "/path/to/game";
        EntityManagerFactory emf = manager.getEntityManagerFactory(gamePath);
        assertTrue(emf.isOpen());
        assertTrue(manager.hasContext(gamePath));
        
        manager.closeEntityManagerFactory(gamePath);
        
        assertFalse(manager.hasContext(gamePath));
    }

    @Test
    void testHasContext() {
        String gamePath = "/path/to/game";
        
        assertFalse(manager.hasContext(gamePath));
        
        manager.getEntityManagerFactory(gamePath);
        
        assertTrue(manager.hasContext(gamePath));
    }

    @Test
    void testGetActiveContexts() {
        String gamePath1 = "/path/to/game1";
        String gamePath2 = "/path/to/game2";
        
        manager.getEntityManagerFactory(gamePath1);
        manager.getEntityManagerFactory(gamePath2);
        
        Map<String, EntityManagerFactory> contexts = manager.getActiveContexts();
        
        assertEquals(2, contexts.size());
        assertTrue(contexts.containsKey(gamePath1));
        assertTrue(contexts.containsKey(gamePath2));
    }

    @Test
    void testGetEntityManagerFactoryWithNullPath() {
        assertThrows(IllegalArgumentException.class, () -> {
            manager.getEntityManagerFactory(null);
        });
    }

    @Test
    void testGetEntityManagerFactoryWithEmptyPath() {
        assertThrows(IllegalArgumentException.class, () -> {
            manager.getEntityManagerFactory("");
        });
    }

    @Test
    void testDeactivateClosesAllContexts() {
        String gamePath1 = "/path/to/game1";
        String gamePath2 = "/path/to/game2";
        
        EntityManagerFactory emf1 = manager.getEntityManagerFactory(gamePath1);
        EntityManagerFactory emf2 = manager.getEntityManagerFactory(gamePath2);
        
        manager.deactivate();
        
        assertFalse(emf1.isOpen());
        assertFalse(emf2.isOpen());
        assertEquals(0, manager.getActiveContexts().size());
    }
}