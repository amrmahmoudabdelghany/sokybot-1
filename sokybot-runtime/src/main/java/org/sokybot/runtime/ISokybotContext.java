package org.sokybot.runtime;

import java.util.Optional;

import org.sokybot.IContextAdapter;


public interface ISokybotContext extends IContextAdapter {

	/*
	 * Removed legacy methods:
	 * getFrameConfigurator()
	 * removeGroupListener()
	 */
	IGroupContext[] getGroups();
    
	String[] listNames();
	
	Optional<IGroupContext> findGroupCtx(String name);
	void installGroup(String groupName, String gamePath, String... options);
	void installGroup(String groupName, String gamePath);
}
