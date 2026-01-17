package org.sokybot.persistence.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sokybot.persistence.entities.Division;
import org.sokybot.persistence.entities.DivisionInfo;
import org.sokybot.persistence.entities.GameInfo;
import org.sokybot.persistence.entities.SilkroadType;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameInfoRepositoryImpl.
 */
class GameInfoRepositoryImplTest {

    @TempDir
    Path tempDir;

    private EntityManagerFactory emf;
    private GameInfoRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        String dbUrl = "jdbc:h2:mem:gameinfotest";
        emf = Persistence.createEntityManagerFactory("sokybot-persistence-unit", 
            java.util.Map.of(
                "javax.persistence.jdbc.url", dbUrl,
                "javax.persistence.jdbc.driver", "org.h2.Driver",
                "javax.persistence.jdbc.user", "sa",
                "javax.persistence.jdbc.password", "",
                "hibernate.hbm2ddl.auto", "create-drop",
                "hibernate.show_sql", "false"
            ));
        
        repository = new GameInfoRepositoryImpl(emf);
    }

    @AfterEach
    void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    void testSaveAndFindByGamePath() {
        GameInfo gameInfo = GameInfo.builder()
                .gamePath("/test/game/path")
                .port(15779)
                .version(1001)
                .build();
        
        repository.save(gameInfo);
        Optional<GameInfo> found = repository.findById("/test/game/path");
        
        assertTrue(found.isPresent());
        assertEquals("/test/game/path", found.get().getGamePath());
        assertEquals(15779, found.get().getPort());
        assertEquals(1001, found.get().getVersion());
    }

    @Test
    void testSaveWithDivisionInfo() {
        DivisionInfo divInfo = new DivisionInfo();
        divInfo.local = (byte) 1;
        Division div = new Division();
        div.setName("Test Division");
        div.addHost("host1.example.com");
        div.addHost("host2.example.com");
        divInfo.addDivision(div);
        
        GameInfo gameInfo = GameInfo.builder()
                .gamePath("/game/with/division")
                .port(15779)
                .version(1001)
                .divisionInfo(divInfo)
                .build();
        
        repository.save(gameInfo);
        Optional<GameInfo> found = repository.findById("/game/with/division");
        
        assertTrue(found.isPresent());
        assertNotNull(found.get().getDivisionInfo());
        assertEquals(1, found.get().getDivisionInfo().getDivisions().size());
        assertEquals("Test Division", found.get().getDivisionInfo().getDivisions().get(0).getName());
    }

    @Test
    void testSaveWithSilkroadType() {
        Map<String, String> typeProps = new HashMap<>();
        typeProps.put("Language", "English");
        typeProps.put("Country", "US");
        SilkroadType silkroadType = new SilkroadType(typeProps);
        
        GameInfo gameInfo = GameInfo.builder()
                .gamePath("/game/with/type")
                .port(15779)
                .version(1001)
                .silkroadType(silkroadType)
                .build();
        
        repository.save(gameInfo);
        Optional<GameInfo> found = repository.findById("/game/with/type");
        
        assertTrue(found.isPresent());
        assertNotNull(found.get().getSilkroadType());
        assertEquals("English", found.get().getSilkroadType().getLanguage());
        assertEquals("US", found.get().getSilkroadType().getCountry());
    }

    @Test
    void testSaveWithAllFields() {
        DivisionInfo divInfo = new DivisionInfo();
        divInfo.local = (byte) 2;
        
        Map<String, String> typeProps = new HashMap<>();
        typeProps.put("Language", "German");
        typeProps.put("Country", "DE");
        SilkroadType silkroadType = new SilkroadType(typeProps);
        
        GameInfo gameInfo = GameInfo.builder()
                .gamePath("/game/full")
                .port(15780)
                .version(1002)
                .divisionInfo(divInfo)
                .silkroadType(silkroadType)
                .build();
        
        repository.save(gameInfo);
        Optional<GameInfo> found = repository.findById("/game/full");
        
        assertTrue(found.isPresent());
        assertEquals(15780, found.get().getPort());
        assertEquals(1002, found.get().getVersion());
        assertNotNull(found.get().getDivisionInfo());
        assertNotNull(found.get().getSilkroadType());
    }

    @Test
    void testUpdateGameInfo() {
        GameInfo original = GameInfo.builder()
                .gamePath("/game/update")
                .port(15779)
                .version(1001)
                .build();
        repository.save(original);
        
        GameInfo updated = GameInfo.builder()
                .gamePath("/game/update")
                .port(15780)
                .version(1002)
                .build();
        repository.save(updated);
        
        Optional<GameInfo> found = repository.findById("/game/update");
        assertTrue(found.isPresent());
        assertEquals(15780, found.get().getPort());
        assertEquals(1002, found.get().getVersion());
    }

    @Test
    void testFindAll() {
        repository.saveAll(Arrays.asList(
            GameInfo.builder().gamePath("/game/1").port(15779).version(1001).build(),
            GameInfo.builder().gamePath("/game/2").port(15780).version(1002).build(),
            GameInfo.builder().gamePath("/game/3").port(15781).version(1003).build()
        ));
        
        List<GameInfo> all = repository.findAll();
        
        assertEquals(3, all.size());
    }

    @Test
    void testDeleteByGamePath() {
        GameInfo gameInfo = GameInfo.builder()
                .gamePath("/game/to/delete")
                .port(15779)
                .version(1001)
                .build();
        repository.save(gameInfo);
        assertTrue(repository.existsById("/game/to/delete"));
        
        repository.deleteById("/game/to/delete");
        
        assertFalse(repository.existsById("/game/to/delete"));
    }

    @Test
    void testExistsById() {
        repository.save(GameInfo.builder()
                .gamePath("/game/exists")
                .port(15779)
                .version(1001)
                .build());
        
        assertTrue(repository.existsById("/game/exists"));
        assertFalse(repository.existsById("/game/does/not/exist"));
    }

    @Test
    void testCount() {
        assertEquals(0, repository.count());
        
        repository.save(GameInfo.builder().gamePath("/game/1").port(15779).version(1001).build());
        assertEquals(1, repository.count());
        
        repository.save(GameInfo.builder().gamePath("/game/2").port(15780).version(1002).build());
        assertEquals(2, repository.count());
    }

    @Test
    void testFindByIdWithSpecialCharacters() {
        String gamePath = "/path/with/special-chars_123";
        GameInfo gameInfo = GameInfo.builder()
                .gamePath(gamePath)
                .port(15779)
                .version(1001)
                .build();
        
        repository.save(gameInfo);
        Optional<GameInfo> found = repository.findById(gamePath);
        
        assertTrue(found.isPresent());
        assertEquals(gamePath, found.get().getGamePath());
    }

    @Test
    void testCascadeDeleteDivisionInfo() {
        DivisionInfo divInfo = new DivisionInfo();
        GameInfo gameInfo = GameInfo.builder()
                .gamePath("/game/cascade")
                .divisionInfo(divInfo)
                .port(15779)
                .version(1001)
                .build();
        repository.save(gameInfo);
        
        repository.deleteById("/game/cascade");
        
        // DivisionInfo should be cascade deleted
        assertFalse(repository.existsById("/game/cascade"));
    }
}