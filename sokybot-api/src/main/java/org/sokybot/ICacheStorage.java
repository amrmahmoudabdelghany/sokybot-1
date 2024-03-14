package org.sokybot;

import java.util.List;

public interface ICacheStorage {

	<V> void store(String key, V val);

	<V> void store(String key, V val, Class<V> as);

	<T> T getValueOrDefault(String key, Class<T> type, T val);

	<T> T getValue(String key, Class<T> type);

	public <T> List<T> getAll(Class<T> type);

	void flush();

	void destroy();

}