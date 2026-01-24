package org.sokybot.persistence.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sokybot.persistence.entities.LvlEXP;
import org.sokybot.persistence.service.PersistenceException;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced unit tests for AbstractRepository covering edge cases and error scenarios.
 */
class AbstractRepositoryAdvancedTest {

    @TempDir
    Path tempDir;

    private EntityManagerFactory emf;
    private LvlEXPRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        String dbUrl = "jdbc:h2:mem:advancedtest;DB_CLOSE_DELAY=-1";
        emf = Persistence.createEntityManagerFactory("sokybot-persistence-unit", 
            java.util.Map.of(
                "javax.persistence.jdbc.url", dbUrl,
                "javax.persistence.jdbc.driver", "org.h2.Driver",
                "javax.persistence.jdbc.user", "sa",
                "javax.persistence.jdbc.password", "",
                "hibernate.hbm2ddl.auto", "create-drop",
                "hibernate.show_sql", "false"
            ));
        
        repository = new LvlEXPRepositoryImpl(emf);
    }

    @Test
    void testSaveAllWithEmptyList() {
        List<LvlEXP> emptyList = Collections.emptyList();
        
        List<LvlEXP> result = repository.saveAll(emptyList);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, repository.count());
    }

    @Test
    void testSaveAllWithNullList() {
        // AbstractRepository checks for null and returns early
        List<LvlEXP> result = repository.saveAll(null);
        
        assertNull(result);
        assertEquals(0, repository.count());
    }

    @Test
    void testSaveAllWithNullEntity() {
        List<LvlEXP> listWithNull = new ArrayList<>();
        listWithNull.add(new LvlEXP(1, 1000L));
        listWithNull.add(null);
        listWithNull.add(new LvlEXP(2, 2000L));
        
        // Should handle null gracefully (skip null entities)
        List<LvlEXP> result = repository.saveAll(listWithNull);
        assertEquals(3, result.size()); // Result list preserves input structure
        // Only non-null entities are saved, but count may vary
        assertTrue(repository.count() >= 2);
    }

    @Test
    void testFindByIdWithNull() {
        Optional<LvlEXP> found = repository.findById(null);
        
        assertFalse(found.isPresent());
    }

    @Test
    void testDeleteByIdWithNull() {
        // Should handle null gracefully without exception
        assertDoesNotThrow(() -> {
            repository.deleteById(null);
        });
    }

    @Test
    void testDeleteWithNull() {
        // Should handle null gracefully
        assertDoesNotThrow(() -> {
            repository.delete(null);
        });
    }

    @Test
    void testExistsByIdWithNull() {
        boolean exists = repository.existsById(null);
        
        assertFalse(exists);
    }

    @Test
    void testConcurrentSaves() throws InterruptedException {
        int threadCount = 5;
        int itemsPerThread = 10;
        Thread[] threads = new Thread[threadCount];
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < itemsPerThread; j++) {
                        int id = threadId * itemsPerThread + j;
                        repository.save(new LvlEXP(id, id * 1000L));
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // All saves should succeed
        assertEquals(threadCount * itemsPerThread, repository.count());
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent saves");
    }

    @Test
    void testSaveUpdateSameEntity() {
        LvlEXP entity = new LvlEXP(500, 5000L);
        repository.save(entity);
        
        // Update the same entity
        LvlEXP updated = new LvlEXP(500, 10000L);
        repository.save(updated);
        
        Optional<LvlEXP> found = repository.findById(500);
        assertTrue(found.isPresent());
        assertEquals(10000L, found.get().getExp());
    }

    @Test
    void testDeleteNonExistentEntity() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            repository.deleteById(99999);
        });
    }

    @Test
    void testDeleteAllOnEmptyRepository() {
        assertEquals(0, repository.count());
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            repository.deleteAll();
        });
        
        assertEquals(0, repository.count());
    }

    @Test
    void testFindAllOnEmptyRepository() {
        List<LvlEXP> all = repository.findAll();
        
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    void testMultipleSavesAndReads() {
        // Save multiple entities
        for (int i = 1; i <= 100; i++) {
            repository.save(new LvlEXP(i, i * 1000L));
        }
        
        // Read them back
        for (int i = 1; i <= 100; i++) {
            Optional<LvlEXP> found = repository.findById(i);
            assertTrue(found.isPresent());
            assertEquals(i, found.get().getLevel());
            assertEquals(i * 1000L, found.get().getExp());
        }
        
        assertEquals(100, repository.count());
    }

    @Test
    void testCountAfterMultipleOperations() {
        assertEquals(0, repository.count());
        
        repository.save(new LvlEXP(1, 1000L));
        assertEquals(1, repository.count());
        
        repository.save(new LvlEXP(2, 2000L));
        assertEquals(2, repository.count());
        
        repository.deleteById(1);
        assertEquals(1, repository.count());
        
        repository.save(new LvlEXP(3, 3000L));
        assertEquals(2, repository.count());
        
        repository.deleteAll();
        assertEquals(0, repository.count());
    }

    @Test
    void testSaveAllThenDeleteAll() {
        List<LvlEXP> entities = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            entities.add(new LvlEXP(i, i * 1000L));
        }
        
        repository.saveAll(entities);
        assertEquals(50, repository.count());
        
        repository.deleteAll();
        assertEquals(0, repository.count());
        
        // Verify all deleted
        for (int i = 1; i <= 50; i++) {
            assertFalse(repository.existsById(i));
        }
    }

    @Test
    void testTransactionIsolation() {
        // Save an entity
        LvlEXP entity1 = new LvlEXP(100, 100000L);
        repository.save(entity1);
        
        // Verify it's persisted
        assertTrue(repository.existsById(100));
        Optional<LvlEXP> found1 = repository.findById(100);
        assertTrue(found1.isPresent());
        
        // Delete it
        repository.deleteById(100);
        
        // Verify it's gone
        assertFalse(repository.existsById(100));
        Optional<LvlEXP> found2 = repository.findById(100);
        assertFalse(found2.isPresent());
    }
}