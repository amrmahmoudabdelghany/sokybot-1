package org.sokybot.persistence.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sokybot.persistence.entities.SectorRef;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SectorRefRepositoryImpl.
 */
class SectorRefRepositoryImplTest {

    @TempDir
    Path tempDir;

    private EntityManagerFactory emf;
    private SectorRefRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        String dbUrl = "jdbc:h2:mem:sectortest;DB_CLOSE_DELAY=-1";
        emf = Persistence.createEntityManagerFactory("sokybot-persistence-unit", 
            java.util.Map.of(
                "javax.persistence.jdbc.url", dbUrl,
                "javax.persistence.jdbc.driver", "org.h2.Driver",
                "javax.persistence.jdbc.user", "sa",
                "javax.persistence.jdbc.password", "",
                "hibernate.hbm2ddl.auto", "create-drop",
                "hibernate.show_sql", "false"
            ));
        
        repository = new SectorRefRepositoryImpl(emf);
    }

    @AfterEach
    void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    void testSaveAndFindBySectorYX() {
        SectorRef sector = SectorRef.builder()
                .sectorYX((short) 0x1234)
                .navObjects(Collections.emptyList())
                .navCells(Collections.emptyList())
                .navBorders(Collections.emptyList())
                .navCellLinks(Collections.emptyList())
                .hightMap(new float[256])
                .build();
        
        repository.save(sector);
        Optional<SectorRef> found = repository.findById((short) 0x1234);
        
        assertTrue(found.isPresent());
        assertEquals((short) 0x1234, found.get().getSectorYX());
    }

    @Test
    void testSaveWithShortId() {
        short sectorYX = (short) 0x5678;
        SectorRef sector = SectorRef.builder()
                .sectorYX(sectorYX)
                .navObjects(new ArrayList<>())
                .navCells(new ArrayList<>())
                .navBorders(new ArrayList<>())
                .navCellLinks(new ArrayList<>())
                .hightMap(null)
                .build();
        
        repository.save(sector);
        
        assertTrue(repository.existsById(sectorYX));
        Optional<SectorRef> found = repository.findById(sectorYX);
        assertTrue(found.isPresent());
        assertEquals(sectorYX, found.get().getSectorYX());
    }

    @Test
    void testFindAllSectors() {
        repository.saveAll(Arrays.asList(
            createSector((short) 0x0001),
            createSector((short) 0x0002),
            createSector((short) 0x0003)
        ));
        
        List<SectorRef> all = repository.findAll();
        
        assertEquals(3, all.size());
    }

    @Test
    void testDeleteBySectorYX() {
        short sectorYX = (short) 0x9999;
        SectorRef sector = createSector(sectorYX);
        repository.save(sector);
        
        assertTrue(repository.existsById(sectorYX));
        
        repository.deleteById(sectorYX);
        
        assertFalse(repository.existsById(sectorYX));
    }

    @Test
    void testExistsById() {
        short sectorYX = (short) 0xAAAA;
        repository.save(createSector(sectorYX));
        
        assertTrue(repository.existsById(sectorYX));
        assertFalse(repository.existsById((short) 0xBBBB));
    }

    @Test
    void testCount() {
        assertEquals(0, repository.count());
        
        repository.save(createSector((short) 0x1000));
        assertEquals(1, repository.count());
        
        repository.save(createSector((short) 0x2000));
        assertEquals(2, repository.count());
    }

    private SectorRef createSector(short sectorYX) {
        return SectorRef.builder()
                .sectorYX(sectorYX)
                .navObjects(Collections.emptyList())
                .navCells(Collections.emptyList())
                .navBorders(Collections.emptyList())
                .navCellLinks(Collections.emptyList())
                .hightMap(null)
                .build();
    }
}