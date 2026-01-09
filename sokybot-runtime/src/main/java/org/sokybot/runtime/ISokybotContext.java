package org.sokybot.runtime;

import java.util.Optional;

import org.sokybot.IContextAdapter;
import org.sokybot.service.IMainFrameConfigurator;

public interface ISokybotContext extends IContextAdapter {

	IMainFrameConfigurator getFrameConfigurator();
	
	IGroupContext[] getGroups();
    
	String[] listNames();
	
	Optional<IGroupContext> findGroupCtx(String name);
	
	void addGroupListener(IGroupListener listener);
	void removeGroupListener(IGroupListener listener);
	void installGroup(String groupName, String gamePath, String... options);
	void installGroup(String groupName, String gamePath);
}
