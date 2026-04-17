package org.sokybot.runtime;

import org.sokybot.engine.IEngine;
import org.sokybot.commons.lifecycle.ISubscriptionScope;

public interface IMachineContext extends IContextAdapter, IGameStateProvider, INetworkController, IEngineController {

    String fullName();

    @Override
    boolean isRunning();

    @Override
    IEngine getEngine();

    @Override
    org.sokybot.proxy.IProxyConnection getProxyConnection();

    @Override
    org.sokybot.gamemodel.IGameModel getGameModel();

    @Override
    String getGroupName();

    @Override
    String getMachineName();

    /**
     * Look up an OSGi service by type from this machine's bundle context.
     */
    <T> T getService(Class<T> serviceClass);

    default ISubscriptionScope getSubscriptionScope() {
        throw new UnsupportedOperationException("subscription scope not exposed by this context");
    }
}
