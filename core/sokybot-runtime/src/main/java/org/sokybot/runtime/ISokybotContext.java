package org.sokybot.runtime;

import java.util.Optional;

public interface ISokybotContext extends IContextAdapter {

	IGroupContext[] getGroups();

	String[] listNames();

	Optional<IGroupContext> findGroupCtx(String name);

	void installGroup(String groupName, String gamePath, String... options);

	void installGroup(String groupName, String gamePath);

	void removeGroup(String groupName);
}
