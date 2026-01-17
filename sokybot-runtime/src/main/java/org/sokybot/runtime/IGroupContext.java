package org.sokybot.runtime;

import java.util.Map;
import java.util.Optional;

import org.sokybot.IContextAdapter;
import org.sokybot.ui.api.IPageViewer;
import org.sokybot.game.navigation.IRuteFinder;
import org.sokybot.service.ISroDAO;

public interface IGroupContext extends IContextAdapter {

	ISroDAO getGameDAO();
	
	IMachineContext[] getMachines();
	
	IPageViewer pageViewer();
	
	Optional<IMachineContext> findMachineCtx(String name);
	
	void addMachineListener(IMachineListener machineListener);
	void removeMachineListener(IMachineListener machineListener);
	void installMachine(String name);
	void installMachine(String name, String... options);
	
	/**
	 * Get the route finder for this group.
	 * Created once per group using IRuteFinderFactory.
	 */
	IRuteFinder getRuteFinder();

    org.sokybot.persistence.service.IGameDataLookup getGameDataLookup();
    
    /**
     * Get the shared translator map for this game.
     * Created once per game (shared across all bots) for memory optimization.
     * Translators are thread-safe and can be used concurrently by multiple bots.
     * 
     * @return Map of opcode to translator instance, or empty map if not available
     */
    Map<Integer, org.sokybot.gameevents.events.core.IPacketTranslator> getTranslators();
	
}

