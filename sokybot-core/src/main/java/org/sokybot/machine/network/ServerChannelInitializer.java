package org.sokybot.machine.network;

import org.slf4j.Logger;
import org.sokybot.network.NetworkPeer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;

@Component
@Scope("prototype")
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel>{

	
	@Autowired
	ApplicationContext ctx ; 
	
	@Autowired
	Logger log ;
	
	@Autowired
	ChannelGroup channelGroup ; 
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		log.info("Initializing server channel ") ; 
		ch.attr(NetworkAttributes.TRANSPORT).set(NetworkPeer.SERVER);
		
		ch.pipeline()
		.addLast(this.ctx.getBean(PacketDecoder.class))
		.addLast(this.ctx.getBean(PacketEncoder.class))
		.addLast(this.ctx.getBean(ClientServerBridge.class))
		.addLast(this.ctx.getBean(MassiveHandler.class))
		.addLast(this.ctx.getBean(SimplePacketPublisher.class)); 
		this.channelGroup.add(ch) ; 
	}
}
