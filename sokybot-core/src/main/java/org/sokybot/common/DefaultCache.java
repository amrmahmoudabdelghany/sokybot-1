package org.sokybot.common;

import java.util.List;

import org.sokybot.ICacheStorage;

public class DefaultCache implements ICacheStorage {

	
	
	
	@Override
	public <V> void store(String key, V val, Class<V> as) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public <V> void store(String key, V val) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> T getValueOrDefault(String key, Class<T> type, T val) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getValue(String key, Class<T> type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> List<T> getAll(Class<T> type) {
	  throw new UnsupportedOperationException() ; 
	}

	@Override
	public void flush() {
		  throw new UnsupportedOperationException() ;
		
	}

	@Override
	public void destroy() {
		  throw new UnsupportedOperationException() ;
		
	}

	
	
	
}
