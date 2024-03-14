package org.sokybot;


import java.util.Optional;

import org.sokybot.service.ISroDAO;

//import org.sokybot.machinegroup.repo.ISroDAO;


public interface IGroupContext extends IContextAdapter {

	
	ISroDAO getGameDAO() ; 
	
	IMachineContext [] getMachines() ; 
	
	IPageViewer pageViewer() ; 
	
	Optional<IMachineContext> findMachineCtx(String name) ; 
	
	void addMachineListener(IMachineListener machineListener) ; 
	void removeMachineListener(IMachineListener machineListener) ; 
	void installMachine(String name) ; 
	void installMachine(String name ,String...options) ;  
}
