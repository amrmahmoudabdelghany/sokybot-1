package org.sokybot.persistence.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.persistence.EntityManagerFactory;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced unit tests for PersistenceContextManagerImpl covering edge cases and concurrency.
 */
class PersistenceContextManagerImplAdvancedTest {

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
    void testConcurrentGetEntityManagerFactory() throws InterruptedException {
        String gamePath = "/path/to/game";
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<EntityManagerFactory> emfs = new ConcurrentLinkedQueue<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    EntityManagerFactory emf = manager.getEntityManagerFactory(gamePath);
                    emfs.add(emf);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        
        // All threads should get the same EMF instance
        EntityManagerFactory first = emfs.peek();
        assertTrue(emfs.stream().allMatch(emf -> emf == first));
        assertEquals(threadCount, emfs.size());
    }

    @Test
    void testMultipleGamesConcurrentAccess() throws InterruptedException {
        int gameCount = 5;
        int threadsPerGame = 3;
        CountDownLatch latch = new CountDownLatch(gameCount * threadsPerGame);
        ConcurrentHashMap<String, EntityManagerFactory> gameEmfs = new ConcurrentHashMap<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(gameCount * threadsPerGame);
        for (int i = 0; i < gameCount; i++) {
            final String gamePath = "/game/path/" + i;
            for (int j = 0; j < threadsPerGame; j++) {
                executor.submit(() -> {
                    try {
                        EntityManagerFactory emf = manager.getEntityManagerFactory(gamePath);
                        gameEmfs.put(gamePath, emf);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        
        // Each game should have its own EMF
        assertEquals(gameCount, gameEmfs.size());
        assertEquals(gameCount, manager.getActiveContexts().size());
    }

    @Test
    void testCloseEntityManagerFactoryWithNullPath() {
        // Should handle null gracefully
        assertDoesNotThrow(() -> {
            manager.closeEntityManagerFactory(null);
        });
    }

    @Test
    void testCloseEntityManagerFactoryTwice() {
        String gamePath = "/path/to/game";
        EntityManagerFactory emf = manager.getEntityManagerFactory(gamePath);
        
        manager.closeEntityManagerFactory(gamePath);
        // Should not throw exception on second close
        assertDoesNotThrow(() -> {
            manager.closeEntityManagerFactory(gamePath);
        });
        
        assertFalse(manager.hasContext(gamePath));
    }

    @Test
    void testGetActiveContextsIsCopy() {
        String gamePath1 = "/game/1";
        String gamePath2 = "/game/2";
        
        manager.getEntityManagerFactory(gamePath1);
        manager.getEntityManagerFactory(gamePath2);
        
        Map<String, EntityManagerFactory> contexts1 = manager.getActiveContexts();
        Map<String, EntityManagerFactory> contexts2 = manager.getActiveContexts();
        
        // Should be different instances (defensive copy)
        assertNotSame(contexts1, contexts2);
        assertEquals(2, contexts1.size());
        assertEquals(2, contexts2.size());
    }

    @Test
    void testSanitizeGamePath() {
        // Test various problematic paths
        String[] problematicPaths = {
            "C:\\Program Files\\Game\\",
            "/home/user/game with spaces/",
            "game@#$%^&*()path",
            "very/long/path/" + "x".repeat(300)
        };
        
        for (String path : problematicPaths) {
            // Should not throw exception
            assertDoesNotThrow(() -> {
                EntityManagerFactory emf = manager.getEntityManagerFactory(path);
                assertNotNull(emf);
            });
        }
    }

    @Test
    void testHasContextWithNullPath() {
        assertFalse(manager.hasContext(null));
    }

    @Test
    void testActivateWithConfig() {
        PersistenceContextManagerImpl newManager = new PersistenceContextManagerImpl();
        Map<String, Object> config = Map.of("db.base.path", tempDir.resolve("custom").toString());
        
        assertDoesNotThrow(() -> {
            newManager.activate(config);
            newManager.deactivate();
        });
    }

    @Test
    void testDeactivateOnEmptyManager() {
        PersistenceContextManagerImpl emptyManager = new PersistenceContextManagerImpl();
        emptyManager.activate(null);
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            emptyManager.deactivate();
        });
    }

    @Test
    void testGetEntityManagerFactoryAfterDeactivate() {
        String gamePath = "/game/path";
        manager.getEntityManagerFactory(gamePath);
        manager.deactivate();
        
        // Creating new manager
        PersistenceContextManagerImpl newManager = new PersistenceContextManagerImpl();
        newManager.activate(null);
        
        // Should create new EMF
        EntityManagerFactory emf = newManager.getEntityManagerFactory(gamePath);
        assertNotNull(emf);
        assertTrue(emf.isOpen());
        
        newManager.deactivate();
    }

    @Test
    void testContextIsolationBetweenGames() {
        String gamePath1 = "/game/1";
        String gamePath2 = "/game/2";
        
        EntityManagerFactory emf1 = manager.getEntityManagerFactory(gamePath1);
        EntityManagerFactory emf2 = manager.getEntityManagerFactory(gamePath2);
        
        assertNotSame(emf1, emf2);
        assertTrue(emf1.isOpen());
        assertTrue(emf2.isOpen());
        
        // Close one should not affect the other
        manager.closeEntityManagerFactory(gamePath1);
        assertFalse(manager.hasContext(gamePath1));
        assertTrue(manager.hasContext(gamePath2));
        assertTrue(emf2.isOpen());
    }
}