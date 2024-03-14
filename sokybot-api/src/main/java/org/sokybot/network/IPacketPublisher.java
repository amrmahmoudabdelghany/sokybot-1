package org.sokybot.network;


public interface IPacketPublisher  {

	public static final int ANY = 0 ; 
	
	
	//public void complete() ; 
	//public void error(Throwable throwable) ; 
	//public void submit(ImmutablePacket packet) ; 
	public IPacketSubscription subscribe(IPacketObserver observer , int opcode ) ; 
	
}
