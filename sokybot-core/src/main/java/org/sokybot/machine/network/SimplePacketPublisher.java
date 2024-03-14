package org.sokybot.machine.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import java.util.concurrent.locks.ReentrantLock;

import org.sokybot.network.IPacketObserver;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.IPacketSubscription;
import org.sokybot.network.packet.ImmutablePacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Service
@Sharable
public class SimplePacketPublisher extends SimpleChannelInboundHandler<ImmutablePacket> implements IPacketPublisher {

	@Autowired
	// @Qualifier("threadPoolTaskExecutor")
	private Executor executor;

	private Map<Integer, List<IPacketObserver>> observers = new HashMap<>();

	private ReentrantLock lock = new ReentrantLock();

	public SimplePacketPublisher() {
		this.observers.put(ANY, new ArrayList<>());
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ImmutablePacket packet) throws Exception {

		List<IPacketObserver> target = this.observers.get(ANY);

		if (target != null) {
			target.stream().forEach((obs) -> executor.execute(() -> obs.onNext(packet.getOpcode(), packet)));
		}

		// comment this code just for test
		// target = this.observers.get(packet.getOpcode());
		// if(target != null) {
		// target.stream()
		// .forEach((obs)->executor.execute(()->obs.onNext(packet.getOpcode(),
		// packet)));
		// }

		
		target = this.observers.get(packet.getOpcode()) ; 
		
		if(target != null) { 
			for(IPacketObserver obs : target) { 
				obs.onNext(packet.getOpcode(), packet);
			}
			
		}
		
		ctx.fireChannelRead(packet);

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

		this.observers.values().stream().flatMap((l) -> l.stream()).forEach((obs) -> {
			this.executor.execute(() -> obs.onError(cause));
		});

		super.exceptionCaught(ctx, cause);
	}

	@Override
	public IPacketSubscription subscribe(IPacketObserver observer, int opcode) {

		List<IPacketObserver> target = this.observers.get(opcode);
		if (target == null) {
			target = new ArrayList<>();
			observers.put(opcode, target);
		}

		target.add(observer);

		return new PacketSubscription(target, observer);
	}

	// @Override
	private void complete() {

		this.observers.values().stream().flatMap((l) -> l.stream()).forEach((obs) -> {
			this.executor.execute(() -> obs.onComplete());
		});
	}

	public class PacketSubscription implements IPacketSubscription {

		private List<IPacketObserver> parentList;
		private IPacketObserver source;

		private PacketSubscription(List<IPacketObserver> parentList, IPacketObserver target) {
			this.parentList = parentList;
			this.source = target;

		}

		@Override
		public void cancel() {
			this.parentList.remove(source);
		}
	}

}
