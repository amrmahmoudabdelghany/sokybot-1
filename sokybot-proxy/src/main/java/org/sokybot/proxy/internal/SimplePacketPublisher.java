package org.sokybot.proxy.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.sokybot.network.IPacketObserver;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.IPacketSubscription;
import org.sokybot.network.packet.ImmutablePacket;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Publishes packets to subscribed observers.
 * Implements the pub/sub pattern for packet handling.
 */
@Sharable
public class SimplePacketPublisher extends SimpleChannelInboundHandler<ImmutablePacket> implements IPacketPublisher {
    
    private final Executor executor;
    private final Map<Integer, List<IPacketObserver>> observers;
    
    public SimplePacketPublisher() {
        this.executor = Executors.newCachedThreadPool();
        this.observers = new HashMap<>();
        this.observers.put(ANY, new ArrayList<>());
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImmutablePacket packet) throws Exception {
        // Notify ANY observers
        List<IPacketObserver> anyObservers = observers.get(ANY);
        if (anyObservers != null) {
            for (IPacketObserver obs : anyObservers) {
                executor.execute(() -> obs.onNext(packet.getOpcode(), packet));
            }
        }
        
        // Notify opcode-specific observers
        List<IPacketObserver> specificObservers = observers.get(packet.getOpcode());
        if (specificObservers != null) {
            for (IPacketObserver obs : specificObservers) {
                obs.onNext(packet.getOpcode(), packet);
            }
        }
        
        ctx.fireChannelRead(packet);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        for (List<IPacketObserver> observerList : observers.values()) {
            for (IPacketObserver obs : observerList) {
                executor.execute(() -> obs.onError(cause));
            }
        }
        super.exceptionCaught(ctx, cause);
    }
    
    @Override
    public IPacketSubscription subscribe(IPacketObserver observer, int opcode) {
        List<IPacketObserver> targetList = observers.get(opcode);
        if (targetList == null) {
            targetList = new ArrayList<>();
            observers.put(opcode, targetList);
        }
        
        targetList.add(observer);
        
        return new PacketSubscription(targetList, observer);
    }
    
    /**
     * Subscription handle that allows unsubscribing.
     */
    private static class PacketSubscription implements IPacketSubscription {
        
        private final List<IPacketObserver> parentList;
        private final IPacketObserver source;
        
        PacketSubscription(List<IPacketObserver> parentList, IPacketObserver source) {
            this.parentList = parentList;
            this.source = source;
        }
        
        @Override
        public void cancel() {
            parentList.remove(source);
        }
    }
}
