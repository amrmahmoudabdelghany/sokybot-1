package org.sokybot.machine.network;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.model.ClientFeed;
import org.sokybot.machine.model.ServerFeed;
import org.sokybot.machine.network.PacketListener.PacketSource;
import org.sokybot.network.IPacketObserver;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ImmutablePacket;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class PacketListenerAdapter implements IPacketObserver {

	private final Object owner;
	private final Method handler;
	private final PacketSource packetSource;

	private final boolean hasRedirect;
	private final boolean hasReturn;

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private StateMachine<MachineState, IMachineEvent> machine;

	public PacketListenerAdapter(Object owner, Method handler, PacketSource packetSource) {
		this.owner = owner;
		this.handler = handler;
		this.packetSource = packetSource;
		Class<?> returnType = this.handler.getReturnType();

		this.hasReturn = (returnType != Void.class);
		this.hasRedirect = (returnType == Message.class);

	}

	@Override
	public void onComplete() {
	}

	@Override
	public void onError(Throwable ex) {
	}

	@Override
	public void onNext(int opcode, ImmutablePacket packet) {

		switch (this.packetSource) {
		case CLIENT:
			if (packet.getPacketSource() == NetworkPeer.SERVER)
				return;
		case SERVER:
			if (packet.getPacketSource() == NetworkPeer.CLIENT)
				return;
		default:
			break;
		}

		Object[] params = Stream.of(this.handler.getParameters()).map((p) -> {
			if (p.getType() == ImmutablePacket.class) {
				return packet;
			} else {

				try {
					return this.ctx.getBean(p.getType());
				} catch (NoSuchBeanDefinitionException e) {
					throw new IllegalStateException(
							"Could not resolve parameter " + p.getName() + "for packet listener " + handler.getName(),
							e);
				}
			}
		}).toArray();

		try {
			Object res = this.handler.invoke(this.owner, params);
			if (this.hasRedirect) {
				this.machine.sendEvent((Message)res) ; 
			} else if (this.hasReturn) {
				IMachineEvent event ; 
				NetworkPeer s = packet.getPacketSource() ; 
				
				if(s == NetworkPeer.SERVER ) { 
					event = ServerFeed.of(packet.getOpcode()); 
				}else  { 
					event = ClientFeed.of(packet.getOpcode()) ; 
					
				}
				
				
				this.machine.sendEvent(MessageBuilder.withPayload(event).setHeader("model", res).build()) ;
				
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {

			e.printStackTrace();
		}

	}
	
	
}
