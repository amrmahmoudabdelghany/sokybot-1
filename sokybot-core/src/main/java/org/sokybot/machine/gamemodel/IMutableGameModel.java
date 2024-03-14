package org.sokybot.machine.gamemodel;

import org.sokybot.machinegroup.gamemodel.ISpawnable;

public interface IMutableGameModel extends IGameModel{

	
	void add(ISpawnable obj) ; 
	ISpawnable  remove(int id) ; 
	public void setSelectedSpawn(int id) ; 
}
