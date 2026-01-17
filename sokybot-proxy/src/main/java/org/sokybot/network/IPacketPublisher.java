package org.sokybot.network;

public interface IPacketPublisher {
    
    IPacketSubscription subscribe(int opcode, IPacketObserver observer);
    
    IPacketSubscription subscribe(IPacketObserver observer, int... opcodes);
}
