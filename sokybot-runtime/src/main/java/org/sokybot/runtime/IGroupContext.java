package org.sokybot.runtime;

import java.util.Optional;


import org.sokybot.ui.api.IPageViewer;
import org.sokybot.game.navigation.IRuteFinder;

public interface IGroupContext extends IContextAdapter {
	
	IMachineContext[] getMachines();
	
	IPageViewer pageViewer();
	
	Optional<IMachineContext> findMachineCtx(String name);
	

	void installMachine(String name);
	void installMachine(String name, String... options);
	
	/**
	 * Get the route finder for this group.
	 * Created once per group using IRuteFinderFactory.
	 */
	IRuteFinder getRuteFinder();

    /**
     * Get the game data lookup for this group.
     * Replaces legacy ISroDAO interface.
     * Created once per group using IGamePersistenceFactory.
     */
    org.sokybot.persistence.service.IGameDataLookup getGameDataLookup();
	
}

