package org.sokybot.runtime;

import org.sokybot.runtime.internal.domain.GroupInfo;
import org.osgi.framework.BundleContext;

public interface IGroupContextFactory {

    IGroupContext createGroupContext(GroupInfo groupInfo, BundleContext bundleContext);
    
    void destroyGroupContext(IGroupContext groupContext);
}
