package org.sokybot.runtime;

import java.util.Optional;

import org.sokybot.game.navigation.IRouteFinder;

public interface IGroupContext extends IContextAdapter {

	IMachineContext[] getMachines();

	Optional<IMachineContext> findMachineCtx(String name);

	void installMachine(String name);

	void installMachine(String name, String... options);

	/**
	 * Get the route finder for this group.
	 * Created once per group using IRouteFinderFactory.
	 */
	IRouteFinder getRouteFinder();

	/**
	 * Get the game data lookup for this group.
	 * Created once per group using IGamePersistenceFactory.
	 */
	org.sokybot.persistence.service.IGameDataLookup getGameDataLookup();

	String getGamePath();

	void removeMachine(String machineName);

}
