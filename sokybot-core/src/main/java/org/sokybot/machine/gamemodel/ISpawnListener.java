package org.sokybot.machine.gamemodel;

import org.sokybot.machinegroup.gamemodel.ISpawnable;

public interface ISpawnListener {

	
	public void spawnAdded(ISpawnable spawnObj ) ; 
	public void spawnRemoved(ISpawnable spawnObj) ; 
	public void spawnSelected(ISpawnable spawnObj) ; 
	
}
