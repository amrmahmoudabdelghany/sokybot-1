package org.sokybot.runtime;

import org.sokybot.engine.IEngine;

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
}
