package org.sokybot.common;

import java.io.Serializable;
import java.util.List;

import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.sokybot.ICacheStorage;


public class CacheStorageImp  implements ICacheStorage{

	
	private PersistentCacheManager cacheManager ; 
	
	
	public CacheStorageImp() { 
		 this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				 .with(CacheManagerBuilder.persistence("./cache"))
				 
				// .withCache(String.class.getTypeName(), CacheConfigurationBuilder
				//	.newCacheConfigurationBuilder(String.class, String.class,
				//	ResourcePoolsBuilder.newResourcePoolsBuilder()
				//	.heap(1000, EntryUnit.ENTRIES)
				//	.offheap(50, MemoryUnit.MB)
				//	.disk(1, MemoryUnit.GB , true)))
				 //.withCache("my-cache", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class , ))
				 .build(true) ; 
		 
		// this.cacheManager.init();
	}
	
	
	
	@Override
	public <V> void store(String key, V val ) {
		
		Class<V> clazz = (Class<V>) val.getClass() ; 
		
		String typeName = val.getClass().getTypeName() ; 
		
		var cache = this.cacheManager.getCache(typeName, String.class, clazz) ;
		
		
		if(cache == null) { 
			
		//	createNewCache(typeName , val.getClass()) ; 
		
			cache = 	this.cacheManager.createCache(typeName,
					CacheConfigurationBuilder
					.newCacheConfigurationBuilder(String.class, clazz,
					ResourcePoolsBuilder.newResourcePoolsBuilder()
				//	.heap(1000, EntryUnit.ENTRIES)
				//	.offheap(50, MemoryUnit.MB)
					.disk(1, MemoryUnit.GB , true)));
			
			//this.cacheManager.init();
		}
		
		cache.put(key, val);
		
		
	  
	}
	
	
	
//	private Cache<String , ? extends Object> createNewCache(String alias  , Class<? extends Object> clazz  ) { 
//		
//	return 	this.cacheManager.createCache(alias,
//				CacheConfigurationBuilder
//				.newCacheConfigurationBuilder(String.class, clazz,
//				ResourcePoolsBuilder.newResourcePoolsBuilder()
//				.heap(10, EntryUnit.ENTRIES)
//				.disk(1, MemoryUnit.GB , true)));
//		
//		
//	}
	

	public <T> T getValueOrDefault(String key, Class<T> type, T val) {
		T obj = getValue(key, type);

		return obj != null ? obj : val;
	}

	public <T> T getValue(String key, Class<T> type) {
		
		String typeName = type.getTypeName(); 
		
		var cache = this.cacheManager.getCache(typeName, String.class, type);
		
		if(cache == null) { 
			
			//	createNewCache(typeName , val.getClass()) ; 
			
				cache = 	this.cacheManager.createCache(typeName,
						CacheConfigurationBuilder
						.newCacheConfigurationBuilder(String.class, type,
						ResourcePoolsBuilder.newResourcePoolsBuilder()
					//	.heap(1000, EntryUnit.ENTRIES)
					//	.offheap(50, MemoryUnit.MB)
						.disk(1, MemoryUnit.GB , true)));
				
				//this.cacheManager.init();
			}
			
		
		T res = null ;
		
		
		if(cache != null) { 
			res = cache.get(key) ;
		}
		
		return res;
	}

	public <T> List<T> getAll(Class<T> type) {
 
		
		
		return null;
	}

	public void flush() {
		this.cacheManager.close(); 
	}

	public void destroy() {
		
	}

	
	public static void main(String args[]) { 
		
		ICacheStorage  cacheStorage   = new CacheStorageImp() ; 
		
		long startTime = System.currentTimeMillis() ; 
		
		
		
		// put 100 student 
		
		
//		for(int i = 0 ; i < 100 ; i++) { 
//			cacheStorage.store("Key"+i, new Student("Student1" , "Id1"));
//		}
		
//		// put 100 String 
//		
//		System.out.println("Put 100 String....") ;
//		 for(int i = 0 ; i < 100 ; i++) { 
//			 cacheStorage.store("Key"+i, "Val" + String.valueOf(i));
//		 }
//		 
		System.out.println("Start retrieving data") ; 
		 
		System.out.println("Retrive 100 String") ; 
		
//		for(int i = 0 ;  i < 100 ; i++) { 
//			String s  = cacheStorage.getValue("Key" + i, String.class) ;
//			System.out.println("Student : " + s) ; 
//		}
//		 
		
		for(int i = 0 ; i <100 ; i++) { 
			Student s = cacheStorage.getValue("Key"+i, Student.class) ; 
			System.out.println(s) ; 
		}
		
		 long duration = (System.currentTimeMillis()  - startTime) ;  
		 System.out.println("Storing data done at " + duration/ 1000  + " seconds , milis  " + duration % 1000) ; 
		 
		cacheStorage.flush() ; 
		 
	}

	
	public static class Student implements Serializable { 
		
		private String name ; 
		
		private String id ; 
		
		
		
		public Student(String name , String id) { 
			this.name = name ; 
			this.id  = id ; 
			
		}



		@Override
		public String toString() {
			return "Student [name=" + name + ", id=" + id + "]";
		}
		
		
		
	}


	
	
	
}
