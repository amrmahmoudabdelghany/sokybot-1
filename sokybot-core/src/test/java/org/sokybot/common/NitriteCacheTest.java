package org.sokybot.common;

import static org.junit.jupiter.api.Assertions.*;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.common.mapper.JacksonMapperModule;
import org.dizitart.no2.common.module.NitriteModule;
import org.dizitart.no2.mvstore.MVStoreModule;
import org.h2.util.geometry.GeometryUtils.DimensionSystemTarget;
import org.junit.jupiter.api.Test;
import org.sokybot.ICacheStorage;
import org.sokybot.machinegroup.pk2extractor.Pk2Extractors;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.sokybot.machinegroup.service.SroMaterialDAO;
import org.sokybot.test.NitriteCache;

class NitriteCacheTest {

	static Nitrite  db() {

		return Nitrite.builder()
				.loadModule(mvstoreModule())
				.loadModule(new JacksonMapperModule())
				.openOrCreate("soky", "soky");

	}

	 static  NitriteModule mvstoreModule() {
		return MVStoreModule.withConfig()
				.filePath("src\\test\\resources\\sokybot-test.data")
				.autoCommit(true)
				//.autoCommitBufferSize(1024)
				//.cacheConcurrency(20)
				//.cacheSize(1024)
				.build();

	}
	 
	static NitriteCollection collection = db().getCollection("game-cache") ; 
	static ICacheStorage cacheStorage = new NitriteCache(collection) ; 
	 static { 
		 collection.clear(); 
	 }
	
	 
	 @Test
	 void testRealInteraction() { 

			ISroMaterialDAO sroDao = 
					new SroMaterialDAO(new Pk2Extractors("E:\\Amroo\\Silkroad Games\\LegionSRO_15_08_2019", cacheStorage), null);

			sroDao.refresh();
	 }
	 @Test
	 void testAddInts() { 
		
		cacheStorage.store("a-int", 20);
		cacheStorage.store("b-int", 30);
		cacheStorage.flush(); 
		assertEquals(cacheStorage.getValue("a-int", Integer.class) , 20);
		assertNotEquals(cacheStorage.getValue("b-int", Integer.class) ,  20);
	 }
	 
	 
	 @Test
	 void testGetInts() { 
		 ICacheStorage cacheStorage = new NitriteCache(db().getCollection("game-cache")) ;
		 
	//	 cacheStorage.getValue(null, null)
			cacheStorage.getAll(Integer.class).forEach((i)->{
				System.out.println(i) ; 
			}); 
	 }
	 

}
