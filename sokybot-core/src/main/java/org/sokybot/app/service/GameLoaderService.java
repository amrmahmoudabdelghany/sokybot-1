package org.sokybot.app.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.sokybot.service.IGameLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GameLoaderService implements IGameLoaderService, ServiceListener {

	private Map<String, IGameLoader> gameLoaders = new ConcurrentHashMap<>();

	@Autowired
	private IPluginService pluginService;

	@PostConstruct
	void init() {
		pluginService.addServiceListener(this, IGameLoader.class);
		pluginService.findService(IGameLoader.class).forEach((loader) -> gameLoaders.put(loader.getName(), loader));

	}

	@Override
	public Optional<IGameLoader> findGameLoader(String name) {
		// TODO Auto-generated method stub
		return Optional.ofNullable(this.gameLoaders.get(name));
	}

	@Override
	public Set<String> listAvailables() {

		return this.gameLoaders.keySet();
	}

	@Override
	public void serviceChanged(ServiceEvent event) {

			this.pluginService.getService(event.getServiceReference())
					.filter((g) -> g instanceof IGameLoader)
					.ifPresent((gameLoader) -> {
						IGameLoader theLoader = (IGameLoader) gameLoader;
						if(event.getType() == ServiceEvent.REGISTERED) { 

							this.gameLoaders.put(theLoader.getName(), theLoader);
							log.info("Game loader {} has been registered", theLoader.getName());	
						}else if(event.getType() == ServiceEvent.UNREGISTERING) { 
							this.gameLoaders.remove(theLoader.getName());
							log.info("Game loader {} has been removed", theLoader.getName());	
						}
					});

			
		

	}

}
