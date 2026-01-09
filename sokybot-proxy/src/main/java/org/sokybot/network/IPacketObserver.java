package org.sokybot.network;

import org.sokybot.network.packet.ImmutablePacket;

public interface IPacketObserver {

	
	public void onNext(int opcode , ImmutablePacket packet) ; 
	public void onComplete() ; 
	public void onError(Throwable ex) ; 
	
}
