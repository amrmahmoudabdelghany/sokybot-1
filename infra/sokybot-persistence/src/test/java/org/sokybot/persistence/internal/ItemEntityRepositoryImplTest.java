package org.sokybot.persistence.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.entities.ItemType;
import org.sokybot.persistence.entities.Race;
import org.sokybot.persistence.entities.Gender;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ItemEntityRepositoryImpl.
 */
class ItemEntityRepositoryImplTest {

    @TempDir
    Path tempDir;

    private EntityManagerFactory emf;
    private ItemEntityRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        String dbUrl = "jdbc:h2:mem:itemtest;DB_CLOSE_DELAY=-1";
        emf = Persistence.createEntityManagerFactory("sokybot-persistence-unit", 
            java.util.Map.of(
                "javax.persistence.jdbc.url", dbUrl,
                "javax.persistence.jdbc.driver", "org.h2.Driver",
                "javax.persistence.jdbc.user", "sa",
                "javax.persistence.jdbc.password", "",
                "hibernate.hbm2ddl.auto", "create-drop",
                "hibernate.show_sql", "false"
            ));
        
        repository = new ItemEntityRepositoryImpl(emf);
    }

    @Test
    void testSaveAndFindById() {
        ItemEntity entity = ItemEntity.builder()
                .refId(1001)
                .longId("ITEM_TEST_001")
                .name("Test Item")
                .itemType(ItemType.HPPot)
                .level(10)
                .build();
        
        ItemEntity saved = repository.save(entity);
        Optional<ItemEntity> found = repository.findById(1001);
        
        assertTrue(found.isPresent());
        assertEquals(1001, found.get().getRefId());
        assertEquals("ITEM_TEST_001", found.get().getLongId());
        assertEquals("Test Item", found.get().getName());
        assertEquals(10, found.get().getLevel());
    }

    @Test
    void testFindItemEntityByLongId() {
        ItemEntity entity = ItemEntity.builder()
                .refId(1002)
                .longId("ITEM_UNIQUE_001")
                .name("Unique Item")
                .build();
        
        repository.save(entity);
        
        Optional<ItemEntity> found = repository.findItemEntityByLongId("ITEM_UNIQUE_001");
        
        assertTrue(found.isPresent());
        assertEquals(1002, found.get().getRefId());
        assertEquals("ITEM_UNIQUE_001", found.get().getLongId());
    }

    @Test
    void testFindItemEntityByLongIdNotFound() {
        Optional<ItemEntity> found = repository.findItemEntityByLongId("NON_EXISTENT");
        
        assertFalse(found.isPresent());
    }

    @Test
    void testSaveWithAllFields() {
        ItemEntity entity = ItemEntity.builder()
                .refId(1003)
                .longId("ITEM_FULL_001")
                .name("Full Item")
                .itemType(ItemType.HPPot)
                .race(Race.Euro)
                .gender(Gender.Male)
                .level(50)
                .degree(5)
                .maxStacks(99)
                .isSortable(true)
                .isSOX(false)
                .isMallItem(true)
                .iconPath("icon/item001.dds")
                .build();
        
        ItemEntity saved = repository.save(entity);
        Optional<ItemEntity> found = repository.findById(1003);
        
        assertTrue(found.isPresent());
        assertEquals(Race.Euro, found.get().getRace());
        assertEquals(Gender.Male, found.get().getGender());
        assertEquals(50, found.get().getLevel());
        assertEquals(5, found.get().getDegree());
        assertTrue(found.get().isMallItem());
    }

    @Test
    void testSaveAll() {
        List<ItemEntity> entities = Arrays.asList(
            ItemEntity.builder().refId(2001).longId("ITEM_001").name("Item 1").build(),
            ItemEntity.builder().refId(2002).longId("ITEM_002").name("Item 2").build(),
            ItemEntity.builder().refId(2003).longId("ITEM_003").name("Item 3").build()
        );
        
        repository.saveAll(entities);
        
        assertEquals(3, repository.count());
        assertTrue(repository.existsById(2001));
        assertTrue(repository.existsById(2002));
        assertTrue(repository.existsById(2003));
    }

    @Test
    void testUpdateExisting() {
        ItemEntity original = ItemEntity.builder()
                .refId(3001)
                .longId("ITEM_UPDATE_001")
                .name("Original Name")
                .level(10)
                .build();
        repository.save(original);
        
        ItemEntity updated = ItemEntity.builder()
                .refId(3001)
                .longId("ITEM_UPDATE_001")
                .name("Updated Name")
                .level(20)
                .build();
        repository.save(updated);
        
        Optional<ItemEntity> found = repository.findById(3001);
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().getName());
        assertEquals(20, found.get().getLevel());
    }

    @Test
    void testDeleteById() {
        ItemEntity entity = ItemEntity.builder()
                .refId(4001)
                .longId("ITEM_DELETE_001")
                .name("To Delete")
                .build();
        repository.save(entity);
        assertTrue(repository.existsById(4001));
        
        repository.deleteById(4001);
        
        assertFalse(repository.existsById(4001));
        assertEquals(0, repository.count());
    }

    @Test
    void testDeleteAll() {
        repository.saveAll(Arrays.asList(
            ItemEntity.builder().refId(5001).longId("ITEM_A").name("A").build(),
            ItemEntity.builder().refId(5002).longId("ITEM_B").name("B").build()
        ));
        assertEquals(2, repository.count());
        
        repository.deleteAll();
        
        assertEquals(0, repository.count());
    }
}