package org.sokybot.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sokybot.ICacheStorage;

public class DefaultCacheStorage implements ICacheStorage {

	private Map<String, Map<String, Object>> memeory = new HashMap<>();

	@Override
	public void destroy() {
		memeory.clear();
	}

	public void flush() {
	}

	@Override
	public void store(String key, Object val) {

		String typeName = val.getClass().getTypeName();
		Map<String, Object> typeMemory = memeory.get(typeName);

		if (typeMemory == null) {
			typeMemory = new HashMap<>();
			memeory.put(typeName, typeMemory);
		}

		typeMemory.put(key, val);
	}

	@Override
	public <T> T getValue(String key, Class<T> type) {

		String typeName = type.getTypeName();
		Map<String, Object> typeMemory = memeory.get(typeName);

		if (typeMemory != null) {

			Object targetVal = typeMemory.get(key);
			if (targetVal != null) {
				return type.cast(targetVal); // we can safely cast this object
			}
		}

		return null;
	}

	@Override
	public <T> T getValueOrDefault(String key, Class<T> type, T val) {
		T obj = getValue(key, type);

		return obj != null ? obj : val;
	}

	@Override
	public <T> List<T> getAll(Class<T> type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> void store(String key, V val, Class<V> as) {
		// TODO Auto-generated method stub
		
	}

}
