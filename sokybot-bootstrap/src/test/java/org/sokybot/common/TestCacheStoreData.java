package org.sokybot.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.common.mapper.JacksonMapperModule;
import org.dizitart.no2.common.module.NitriteModule;
import org.dizitart.no2.filters.FluentFilter;
import org.dizitart.no2.mvstore.MVStoreModule;
import org.dizitart.no2.repository.ObjectRepository;
import org.h2.mvstore.FileStore;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.sokybot.ICacheStorage;
import org.sokybot.app.AppConstants;
import org.sokybot.common.TestCacheStorageImp.DataEntryList;
import org.sokybot.domain.map.NavBorder;
import org.sokybot.domain.map.NavCell;
import org.sokybot.domain.map.NavCellLink;
import org.sokybot.domain.map.NavMesh;
import org.sokybot.domain.map.NavObject;
import org.sokybot.domain.map.Position;
import org.sokybot.domain.map.Segment;
import org.sokybot.machinegroup.service.SroMaterialDAO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestCacheStoreData {

	static Nitrite db() {

	
		return Nitrite.builder()
				
				
				.loadModule(mvstoreModule())
				.loadModule(new JacksonMapperModule())
				
				.openOrCreate("soky", "soky");

	}

	static NitriteModule mvstoreModule() {
		return MVStoreModule.withConfig()
			//	.compress(true)
				//.compressHigh(true)
				.filePath("src\\test\\resources\\sokybot-test3.db")
				.autoCommit(true)
				.cacheConcurrency(8)
				
				.build();

	}

	Nitrite db = db();

	TestCacheStorageImp cache = new TestCacheStorageImp(db);

	ICacheStorage cache2 = new CacheStorageImp() ; 
	
	// @Test
	// public void testRealInteraction() {
	// 	// Removed legacy test
	// }

	@Test
	public void testRetriveAllData() {
		int len = 65000;
		for (int i = 0; i < len; i++) {
			String key = "seg-" + i;
			Segment seg = cache2.getValue(key, Segment.class);
			System.out.println("Check " + key);
			System.out.println("Segement was : " + seg) ; 
			assertNotNull(seg);
		}
	}

	@Test
	public void testAddData() {

		List<Segment> segList = createSegmentList();

		for (int i = 0; i < segList.size(); i++) {
			String key = "seg-" + i;

			Segment target = segList.get(i) ; 
			cache2.store(key, target);

		}

		cache2.flush();

		// check stored keys
		// NitriteCollection coll = this.db.getCollection(Constants.DB_META) ;
		// Document keysDoc =
		// coll.find(FluentFilter.where(TestCacheStorageImp.STORAGE_META).notEq(null)).firstOrNull()
		// ;

		// keysDoc.forEach((p)->{
		// System.out.println(p.getFirst() + " , " + p.getSecond()) ;

		// });
		// assertEquals( segList.size() + 4 ,keysDoc.size() ) ;

	}

	
	
	
	@Test
	public void testOverrideStrings() { 
		
		String testStr = "teststr" ; 
		
		this.cache.store("MyStr", testStr);

		this.cache.flush();

		String testStr2 = this.cache.getValue("MyStr", String.class);

		assertEquals(testStr2, testStr);
		
		this.cache.store("MyStr", "testStr2");
		this.cache.flush();
		
		 testStr2 = this.cache.getValue("MyStr", String.class) ; 
		 
		 assertTrue(testStr2.equals("testStr2"));
		 
		
	}
	
	@Test
	public void testOverrideEntry() {
		Segment seg = createRandomSegment();

		this.cache.store("MySeg", seg);

		this.cache.flush();

		Segment seg2 = this.cache.getValue("MySeg", Segment.class);

		assertEquals(seg2, seg);
		

	}

	private List<Segment> createSegmentList() {
		List<Segment> res = new ArrayList<>();
		int len = 65000;
		for (int i = 0; i < len; i++) {
			res.add(createRandomSegment());

		}
		return res;
	}

	private static Random rnd = new Random();

	private Segment createRandomSegment() {

		
		return Segment.builder().hightMap(new float[1024]).sectorYX((short) rnd.nextInt()).navMesh(new NavMesh(cache)).build(); 
		
		/*
		return Segment.builder()
				.hightMap(new float[1024])
				.sectorYX((short) rnd.nextInt())
				.navBorders(createNavBorderList())
				.navCellLinks(createNavCellLinkList())
				.navCells(createNavCellList())
				.navObjects(createNavObjectList())
				.build();
	*/
	}

	private List<NavObject> createNavObjectList() {
		int len = rnd.nextInt(6);
		List<NavObject> res = new ArrayList<>();
		for (int i = 0; i < len; i++) {
			res.add(createNavObject());
		}
		return res;
	}

	private NavObject createNavObject() {
		return NavObject.builder()
				.collisionFlag((short) rnd.nextInt())
				.eventZoneFlag((short) rnd.nextInt())
				.id(rnd.nextInt())
				.position(new Position(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()))
				.regionID((short) rnd.nextInt())
				.scale((short) rnd.nextInt())
				.uniqueId((short) rnd.nextInt())
				.build();
	}

	private java.util.Vector<Short> createShortList() {
		int len = rnd.nextInt(6);
		Vector<Short> res = new Vector<>();

		for (int i = 0; i < len; i++) {
			res.add((short) rnd.nextInt());
		}
		return res;
	}

	private List<NavCell> createNavCellList() {
		int len = rnd.nextInt(6);

		List<NavCell> res = new ArrayList<>();
		for (int i = 0; i < len; i++) {
			res.add(createNavCell());
		}
		return res;
	}

	private NavCell createNavCell() {
		return new NavCell(new Position(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()),
				new Position(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()), createShortList(), createShortList(),
				createShortList());
	}

	private List<NavBorder> createNavBorderList() {
		int len = rnd.nextInt(6);
		List<NavBorder> res = new ArrayList<>();
		for (int i = 0; i < len; i++) {
			res.add(createRandomNavBorder());

		}
		return res;
	}

	private List<NavCellLink> createNavCellLinkList() {
		int len = rnd.nextInt(6);
		List<NavCellLink> res = new ArrayList<>();
		for (int i = 0; i < len; i++) {
			res.add(createNavCellLink());
		}
		return res;
	}

	private NavCellLink createNavCellLink() {
		return NavCellLink.builder()
				.cellDestinationSector((short) rnd.nextInt())
				.cellSourceSector((short) rnd.nextInt())
				.lineDestination((byte) rnd.nextInt())
				.lineSource((byte) rnd.nextInt())
				.lineFlag((byte) rnd.nextInt())
				.max(new Position(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()))
				.min(new Position(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()))
				.build();
	}

	private NavBorder createRandomNavBorder() {

		return NavBorder.builder()
				.cellDestinationIndex((short) rnd.nextInt())
				.cellSourceIndex((short) rnd.nextInt())
				.lineDestination((byte) rnd.nextInt())
				.lineFlag((byte) rnd.nextInt())
				.lineSource((byte) rnd.nextInt())
				.max(new Position(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()))
				.min(new Position(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()))
				.regionDestionationYX((short) rnd.nextInt())
				.regionSourceYX((short) rnd.nextInt())
				.build();

	}

}
