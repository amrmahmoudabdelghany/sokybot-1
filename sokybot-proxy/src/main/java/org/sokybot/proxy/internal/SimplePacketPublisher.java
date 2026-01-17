package org.sokybot.proxy.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.sokybot.network.IPacketObserver;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.IPacketSubscription;
import org.sokybot.network.packet.ImmutablePacket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class SimplePacketPublisher extends SimpleChannelInboundHandler<ImmutablePacket> implements IPacketPublisher {

    private final Map<Integer, CopyOnWriteArrayList<IPacketObserver>> observers = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImmutablePacket packet) throws Exception {
        publish(packet);
        ctx.fireChannelRead(packet);
    }

    @Override
    public IPacketSubscription subscribe(int opcode, IPacketObserver observer) {
        observers.computeIfAbsent(opcode, k -> new CopyOnWriteArrayList<>()).add(observer);

        return () -> {
            CopyOnWriteArrayList<IPacketObserver> list = observers.get(opcode);
            if (list != null) {
                list.remove(observer);
            }
        };
    }

    @Override
    public IPacketSubscription subscribe(IPacketObserver observer, int... opcodes) {
        // Composite subscription could be implemented here
        // For simplicity, we can just subscribe individually but returning a composite subscription
        // simplifies management for consumers.
        
        // However, standard simplistic implementation for now:
        for (int opcode : opcodes) {
            subscribe(opcode, observer);
        }
        
        return () -> {
            for (int opcode : opcodes) {
                CopyOnWriteArrayList<IPacketObserver> list = observers.get(opcode);
                if (list != null) {
                    list.remove(observer);
                }
            }
        };
    }

    public void publish(ImmutablePacket packet) {
        CopyOnWriteArrayList<IPacketObserver> list = observers.get(packet.getOpcode());
        if (list != null) {
            for (IPacketObserver observer : list) {
                try {
                    observer.onPacket(packet);
                } catch (Exception e) {
                   e.printStackTrace(); // Log error but don't stop others
                }
            }
        }
    }
}
