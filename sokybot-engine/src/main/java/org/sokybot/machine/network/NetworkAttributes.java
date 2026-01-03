package org.sokybot.machine.network ; 

import org.sokybot.network.NetworkPeer;

import io.netty.util.AttributeKey;

public class NetworkAttributes {

	public static final AttributeKey<NetworkPeer> TRANSPORT = AttributeKey.newInstance("transport") ;  
}
