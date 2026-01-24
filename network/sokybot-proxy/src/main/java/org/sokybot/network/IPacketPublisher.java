package org.sokybot.network;

public interface IPacketPublisher {
    
    IPacketSubscription subscribe(int opcode, IPacketObserver observer);
    
    IPacketSubscription subscribe(IPacketObserver observer, int... opcodes);
    
    /**
     * Subscribe to all packets regardless of opcode.
     * @param observer the observer to receive all packets
     * @return a subscription handle for unsubscribing
     */
    IPacketSubscription subscribeAll(IPacketObserver observer);
}
