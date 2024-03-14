package org.sokybot;

import java.util.Optional;
import java.util.Set;

import org.sokybot.service.IMainFrameConfigurator;

public interface ISokybotContext  extends IContextAdapter {

	
	
	IMainFrameConfigurator getFrameConfigurator() ; 
	
	IGroupContext[] getGroups() ; 
    
	String[] listNames() ; 
	
	Optional<IGroupContext> findGroupCtx(String name) ; 
	
	void addGroupListener(IGroupListener listener)  ;
	void removeGroupListener(IGroupListener listener) ; 
	void installGroup(String groupName , String gamePath , String...options) ; 
	void installGroup(String groupName , String gamePath ) ; 
	
}
