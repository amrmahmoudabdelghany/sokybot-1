package org.sokybot.app.service;

import java.util.List;
import java.util.Optional;

import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public interface IPluginService {

	<T> List<T> findService(Class<T> serviceType);

	void addServiceListener(ServiceListener listener, Class<?> targetType);

	public <T> Optional<T> getService(ServiceReference<T> ref);
	
	 <T> void  deployService(Class<T> clazz , T service) ; 
	
	
}
