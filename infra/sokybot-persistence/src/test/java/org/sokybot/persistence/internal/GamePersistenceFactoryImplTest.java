package org.sokybot.persistence.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.persistence.service.IPersistenceContextManager;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GamePersistenceFactoryImpl.
 */
class GamePersistenceFactoryImplTest {

    @TempDir
    Path tempDir;

    private IPersistenceContextManager contextManager;
    private GamePersistenceFactoryImpl factory;

    @BeforeEach
    void setUp() {
        System.setProperty("sokybot.db.path", tempDir.toString());
        contextManager = new PersistenceContextManagerImpl();
        ((PersistenceContextManagerImpl) contextManager).activate(null);
        
        factory = new GamePersistenceFactoryImpl(contextManager);
    }

    @AfterEach
    void tearDown() {
        if (contextManager != null) {
            ((PersistenceContextManagerImpl) contextManager).deactivate();
        }
    }

    @Test
    void testRegisterGame() {
        String gamePath = "/path/to/game";
        
        IGameDataLookup lookup = factory.registerGame(gamePath);
        
        assertNotNull(lookup);
        assertEquals(gamePath, lookup.getGamePath());
    }

    @Test
    void testRegisterGameReturnsSameInstance() {
        String gamePath = "/path/to/game";
        
        IGameDataLookup lookup1 = factory.registerGame(gamePath);
        IGameDataLookup lookup2 = factory.registerGame(gamePath);
        
        assertSame(lookup1, lookup2); // Should return same instance
    }

    @Test
    void testGetLookup() {
        String gamePath = "/path/to/game";
        
        assertNull(factory.getLookup(gamePath));
        
        IGameDataLookup lookup = factory.registerGame(gamePath);
        
        assertSame(lookup, factory.getLookup(gamePath));
    }

    @Test
    void testUnregisterGame() {
        String gamePath = "/path/to/game";
        
        factory.registerGame(gamePath);
        assertNotNull(factory.getLookup(gamePath));
        assertTrue(contextManager.hasContext(gamePath));
        
        factory.unregisterGame(gamePath);
        
        assertNull(factory.getLookup(gamePath));
        assertFalse(contextManager.hasContext(gamePath));
    }

    @Test
    void testRegisterMultipleGames() {
        String gamePath1 = "/path/to/game1";
        String gamePath2 = "/path/to/game2";
        
        IGameDataLookup lookup1 = factory.registerGame(gamePath1);
        IGameDataLookup lookup2 = factory.registerGame(gamePath2);
        
        assertNotSame(lookup1, lookup2);
        assertEquals(gamePath1, lookup1.getGamePath());
        assertEquals(gamePath2, lookup2.getGamePath());
    }

    @Test
    void testRegisterGameWithNullContextManager() {
        GamePersistenceFactoryImpl factoryWithoutManager = new GamePersistenceFactoryImpl();
        
        assertThrows(IllegalStateException.class, () -> {
            factoryWithoutManager.registerGame("/path/to/game");
        });
    }
}