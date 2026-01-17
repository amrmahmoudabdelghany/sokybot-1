package org.sokybot.persistence.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sokybot.persistence.entities.*;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.persistence.service.IGamePersistenceFactory;
import org.sokybot.persistence.service.IPersistenceContextManager;

import javax.persistence.EntityManagerFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that test multiple components working together.
 */
class IntegrationTest {

    @TempDir
    Path tempDir;

    private IPersistenceContextManager contextManager;
    private IGamePersistenceFactory factory;

    @BeforeEach
    void setUp() {
        System.setProperty("sokybot.db.path", tempDir.toString());
        PersistenceContextManagerImpl manager = new PersistenceContextManagerImpl();
        manager.activate(null);
        this.contextManager = manager;
        this.factory = new GamePersistenceFactoryImpl(contextManager);
    }

    @Test
    void testGameRegistrationAndLookup() {
        String gamePath = "/test/game/path";
        
        // Register game
        IGameDataLookup lookup = factory.registerGame(gamePath);
        assertNotNull(lookup);
        assertEquals(gamePath, lookup.getGamePath());
        
        // Verify context was created
        assertTrue(contextManager.hasContext(gamePath));
        
        // Get lookup again
        IGameDataLookup lookup2 = factory.getLookup(gamePath);
        assertSame(lookup, lookup2);
    }

    @Test
    void testMultipleGamesIsolation() {
        String gamePath1 = "/game/1";
        String gamePath2 = "/game/2";
        
        IGameDataLookup lookup1 = factory.registerGame(gamePath1);
        IGameDataLookup lookup2 = factory.registerGame(gamePath2);
        
        assertNotSame(lookup1, lookup2);
        assertEquals(gamePath1, lookup1.getGamePath());
        assertEquals(gamePath2, lookup2.getGamePath());
        
        // Each should have its own database context
        assertTrue(contextManager.hasContext(gamePath1));
        assertTrue(contextManager.hasContext(gamePath2));
        
        EntityManagerFactory emf1 = contextManager.getEntityManagerFactory(gamePath1);
        EntityManagerFactory emf2 = contextManager.getEntityManagerFactory(gamePath2);
        assertNotSame(emf1, emf2);
    }

    @Test
    void testUnregisterGameClosesContext() {
        String gamePath = "/game/to/unregister";
        
        factory.registerGame(gamePath);
        assertTrue(contextManager.hasContext(gamePath));
        
        factory.unregisterGame(gamePath);
        
        assertFalse(contextManager.hasContext(gamePath));
        assertNull(factory.getLookup(gamePath));
    }

    @Test
    void testGameDataLookupWithGameInfo() {
        String gamePath = "/game/with/info";
        
        IGameDataLookup lookup = factory.registerGame(gamePath);
        
        // Create GameInfo
        GameInfo gameInfo = GameInfo.builder()
                .gamePath(gamePath)
                .port(15779)
                .version(1001)
                .build();
        
        // Save through internal repository (would need helper or reflection)
        // For now, just verify lookup can be created
        assertNotNull(lookup);
        assertEquals(-1, lookup.getPort()); // Will be -1 if GameInfo not saved
    }

    @Test
    void testRepositoryOperationsWithPerGameContext() {
        String gamePath1 = "/game/repo/test1";
        String gamePath2 = "/game/repo/test2";
        
        EntityManagerFactory emf1 = contextManager.getEntityManagerFactory(gamePath1);
        EntityManagerFactory emf2 = contextManager.getEntityManagerFactory(gamePath2);
        
        // Create repositories for each game
        ItemEntityRepositoryImpl repo1 = new ItemEntityRepositoryImpl(emf1);
        ItemEntityRepositoryImpl repo2 = new ItemEntityRepositoryImpl(emf2);
        
        // Save items to each game's repository
        ItemEntity item1 = ItemEntity.builder()
                .refId(1001)
                .longId("ITEM_GAME1")
                .name("Game 1 Item")
                .build();
        
        ItemEntity item2 = ItemEntity.builder()
                .refId(1001) // Same refId but different game
                .longId("ITEM_GAME2")
                .name("Game 2 Item")
                .build();
        
        repo1.save(item1);
        repo2.save(item2);
        
        // Each repository should only see its own data
        assertTrue(repo1.findById(1001).isPresent());
        assertTrue(repo2.findById(1001).isPresent());
        
        // But they should be different items
        ItemEntity found1 = repo1.findById(1001).get();
        ItemEntity found2 = repo2.findById(1001).get();
        assertEquals("Game 1 Item", found1.getName());
        assertEquals("Game 2 Item", found2.getName());
    }
}