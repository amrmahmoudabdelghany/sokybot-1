package org.sokybot.runtime;

import org.sokybot.proxy.IProxyConnection;

/**
 * Interface for components that provide access to network control.
 */
public interface INetworkController {

    /**
     * Gets the proxy connection.
     * 
     * @return The proxy connection instance.
     */
    IProxyConnection getProxyConnection();
}
