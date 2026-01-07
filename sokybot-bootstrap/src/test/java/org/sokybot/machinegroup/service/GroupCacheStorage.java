package org.sokybot.machinegroup.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ehcache.Cache;
import org.ehcache.CachePersistenceException;
import org.ehcache.PersistentCacheManager;
import org.ehcache.Status;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.sokybot.ICacheStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

//@Service
public class GroupCacheStorage implements ICacheStorage, SmartLifecycle {

	private PersistentCacheManager cacheManager;

	
	
	public GroupCacheStorage(@Value("${groupName}")String groupName ) { 
		if(groupName == null) { 
			groupName = "group-cache" ; 
		}
		this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.with(CacheManagerBuilder.persistence("./sokybot-cache/" + groupName))
				.build();
	
	}
	
	@Override
	public void start() {
		this.cacheManager.init();
	}

	@Override
	public void stop() {
		this.cacheManager.close();

	}

	@Override
	public boolean isRunning() {
		return this.cacheManager.getStatus() == Status.AVAILABLE;
	}

	private Set<Class<?>> getUntilObj(Class<?> clazz) {
		Set<Class<?>> res = new HashSet<>();

		Class<?> current = clazz;

		while (current != Object.class) {
			res.add(current);
			current = current.getSuperclass();
		}

		return res;
	}
	
	
	@Override
	public <V> void store(String key , V val , Class<V> as) { 
		
		     
			String typeName = as.getTypeName() ; 
			Cache<String, V> cache = this.cacheManager.getCache(typeName, String.class, as) ; 
			if(cache == null) { 
				cache = this.cacheManager.createCache(typeName,
						CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, as,
								ResourcePoolsBuilder.newResourcePoolsBuilder()
										// .heap(1000, EntryUnit.ENTRIES)
										// .offheap(50, MemoryUnit.MB)
										.disk(1, MemoryUnit.GB, true)));

			}
			
			
			cache.put(key, as.cast(val)) ; 
			
			//cache.put(key, val) ; 
		
			
	}
	
	@Override
	public <V> void store(String key, V val) {
		Class<V> clazz = (Class<V>) val.getClass();

		String typeName = val.getClass().getTypeName();
		var cache = this.cacheManager.getCache(typeName, String.class, clazz);

		if (cache == null) {

			// createNewCache(typeName , val.getClass()) ;

			cache = this.cacheManager.createCache(typeName,
					CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, clazz,
							ResourcePoolsBuilder.newResourcePoolsBuilder()
									// .heap(1000, EntryUnit.ENTRIES)
									// .offheap(50, MemoryUnit.MB)
									.disk(1, MemoryUnit.GB, true)));

			// this.cacheManager.init();
			
		}

		
		cache.put(key, val);

		
	}

	@Override
	public <T> T getValueOrDefault(String key, Class<T> type, T val) {
		T obj = getValue(key, type);

		return obj != null ? obj : val;
	}

	@Override
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

	@Override
	public <T> List<T> getAll(Class<T> type) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void flush() {

		//throw new UnsupportedOperationException();
	}

	@Override
	public void destroy() {
		try {
			this.cacheManager.destroy();
		} catch (CachePersistenceException e) {

			e.printStackTrace();
		}
	}

}
