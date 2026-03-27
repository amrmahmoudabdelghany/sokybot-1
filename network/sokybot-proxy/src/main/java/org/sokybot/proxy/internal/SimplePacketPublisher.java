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
    private final CopyOnWriteArrayList<IPacketObserver> globalObservers = new CopyOnWriteArrayList<>();

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
        // For simplicity, we can just subscribe individually but returning a composite
        // subscription
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

    @Override
    public IPacketSubscription subscribeAll(IPacketObserver observer) {
        globalObservers.add(observer);
        ClassLoader cl = observer.getClass().getClassLoader();
        System.out.println("[Java] SimplePacketPublisher: subscribeAll observer="
                + observer.getClass().getName()
                + ", classLoader=" + (cl == null ? "bootstrap" : cl.getClass().getName())
                + ", totalGlobalObservers=" + globalObservers.size());
        return () -> globalObservers.remove(observer);
    }

    public void publish(ImmutablePacket packet) {
        // Notify global observers (subscribed to all packets)
        if (!globalObservers.isEmpty()) {
            System.out.println("[Java] SimplePacketPublisher: publishing packet 0x"
                    + Integer.toHexString(packet.getOpcode()) + " to " + globalObservers.size() + " global observers");
        }
        for (IPacketObserver observer : globalObservers) {
            try {
                observer.onPacket(packet);
            } catch (Exception e) {
                System.err.println("[Java] SimplePacketPublisher: global observer "
                        + observer.getClass().getName() + " threw "
                        + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Notify opcode-specific observers
        CopyOnWriteArrayList<IPacketObserver> list = observers.get(packet.getOpcode());
        if (list != null) {
            for (IPacketObserver observer : list) {
                try {
                    observer.onPacket(packet);
                } catch (Exception e) {
                    System.err.println("[Java] SimplePacketPublisher: opcode observer "
                            + observer.getClass().getName() + " threw "
                            + e.getClass().getName() + ": " + e.getMessage());
                    e.printStackTrace(); // Log error but don't stop others
                }
            }
        }
    }
}
