package org.sokybot.persistence.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.entities.NPCType;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NPCEntityRepositoryImpl.
 */
class NPCEntityRepositoryImplTest {

    @TempDir
    Path tempDir;

    private EntityManagerFactory emf;
    private NPCEntityRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        String dbUrl = "jdbc:h2:mem:npc test;DB_CLOSE_DELAY=-1";
        emf = Persistence.createEntityManagerFactory("sokybot-persistence-unit", 
            java.util.Map.of(
                "javax.persistence.jdbc.url", dbUrl,
                "javax.persistence.jdbc.driver", "org.h2.Driver",
                "javax.persistence.jdbc.user", "sa",
                "javax.persistence.jdbc.password", "",
                "hibernate.hbm2ddl.auto", "create-drop",
                "hibernate.show_sql", "false"
            ));
        
        repository = new NPCEntityRepositoryImpl(emf);
    }

    @Test
    void testSaveAndFindById() {
        NPCEntity entity = NPCEntity.builder()
                .refId(5001)
                .longId("NPC_TEST_001")
                .name("Test NPC")
                .level(50)
                .HP(10000)
                .Type(NPCType.Monster)
                .iconPath("icon/npc001.dds")
                .build();
        
        repository.save(entity);
        Optional<NPCEntity> found = repository.findById(5001);
        
        assertTrue(found.isPresent());
        assertEquals(5001, found.get().getRefId());
        assertEquals("Test NPC", found.get().getName());
        assertEquals(50, found.get().getLevel());
        assertEquals(NPCType.Monster, found.get().getType());
    }

    @Test
    void testFindAllMonsterLike() {
        repository.saveAll(Arrays.asList(
            NPCEntity.builder().refId(6001).longId("NPC_001").name("Goblin Warrior").level(10).build(),
            NPCEntity.builder().refId(6002).longId("NPC_002").name("Goblin Shaman").level(15).build(),
            NPCEntity.builder().refId(6003).longId("NPC_003").name("Orc Fighter").level(20).build(),
            NPCEntity.builder().refId(6004).longId("NPC_004").name("Dragon").level(100).build()
        ));
        
        List<NPCEntity> goblins = repository.findAllMonsterLike("Goblin");
        
        assertEquals(2, goblins.size());
        assertTrue(goblins.stream().allMatch(npc -> npc.getName().contains("Goblin")));
    }

    @Test
    void testFindAllMonsterLikeCaseInsensitive() {
        repository.saveAll(Arrays.asList(
            NPCEntity.builder().refId(7001).longId("NPC_A").name("Dragon").level(100).build(),
            NPCEntity.builder().refId(7002).longId("NPC_B").name("DRAGON").level(100).build(),
            NPCEntity.builder().refId(7003).longId("NPC_C").name("dragon").level(100).build()
        ));
        
        List<NPCEntity> dragons = repository.findAllMonsterLike("dragon");
        
        assertEquals(3, dragons.size());
    }

    @Test
    void testFindAllMonsterLikeEmptyFilter() {
        repository.saveAll(Arrays.asList(
            NPCEntity.builder().refId(8001).longId("NPC_X").name("Monster 1").build(),
            NPCEntity.builder().refId(8002).longId("NPC_Y").name("Monster 2").build()
        ));
        
        List<NPCEntity> all = repository.findAllMonsterLike("");
        
        assertEquals(2, all.size());
    }

    @Test
    void testFindAllMonsterLikeNoMatch() {
        repository.save(NPCEntity.builder()
                .refId(9001)
                .longId("NPC_Z")
                .name("Existing NPC")
                .build());
        
        List<NPCEntity> results = repository.findAllMonsterLike("NonExistent");
        
        assertTrue(results.isEmpty());
    }

    @Test
    void testSaveAll() {
        List<NPCEntity> entities = Arrays.asList(
            NPCEntity.builder().refId(10001).longId("NPC_BATCH_001").name("NPC 1").build(),
            NPCEntity.builder().refId(10002).longId("NPC_BATCH_002").name("NPC 2").build(),
            NPCEntity.builder().refId(10003).longId("NPC_BATCH_003").name("NPC 3").build()
        );
        
        repository.saveAll(entities);
        
        assertEquals(3, repository.count());
    }

    @Test
    void testExistsById() {
        repository.save(NPCEntity.builder()
                .refId(11001)
                .longId("NPC_EXISTS")
                .name("Exists")
                .build());
        
        assertTrue(repository.existsById(11001));
        assertFalse(repository.existsById(99999));
    }

    @Test
    void testDelete() {
        NPCEntity entity = NPCEntity.builder()
                .refId(12001)
                .longId("NPC_DELETE")
                .name("To Delete")
                .build();
        repository.save(entity);
        
        repository.delete(entity);
        
        assertFalse(repository.existsById(12001));
    }
}