package org.sokybot.machine.network;

import org.slf4j.Logger;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.model.ClientFeed;
import org.sokybot.network.NetworkPeer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;

@Component
@Scope("prototype")
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

	
	@Autowired
	Logger log ;
	
	@Autowired
	ApplicationContext ctx ; 
	
	@Autowired
	StateMachine<MachineState, IMachineEvent> machine;

	@Autowired
	ChannelGroup channelGroup ;
	
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {

		ch.parent().close().sync() ; 
		
		log.info("Initializing client channel ") ; 
		ch.attr(NetworkAttributes.TRANSPORT).set(NetworkPeer.CLIENT);
		
		ch.pipeline()
		.addLast(this.ctx.getBean(PacketDecoder.class))
		.addLast(this.ctx.getBean(PacketEncoder.class))
		.addLast(this.ctx.getBean(ClientServerBridge.class))
		.addLast(this.ctx.getBean(SimplePacketPublisher.class))
		.addLast(this.ctx.getBean(ClientHandler.class));
		
		this.channelGroup.add(ch) ; 
		
		
	}
	
}

