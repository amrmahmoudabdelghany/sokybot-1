package org.sokybot.proxy.internal;

import org.sokybot.network.NetworkPeer;

import io.netty.util.AttributeKey;

/**
 * Channel attributes for identifying the peer type (CLIENT or SERVER).
 */
public class NetworkAttributes {
    
    public static final AttributeKey<NetworkPeer> TRANSPORT = AttributeKey.newInstance("transport");
}
