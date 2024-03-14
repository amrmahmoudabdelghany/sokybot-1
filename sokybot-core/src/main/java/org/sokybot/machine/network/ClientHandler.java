package org.sokybot.machine.network;

import org.slf4j.Logger;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.model.ClientFeed;
import org.sokybot.network.packet.ImmutablePacket;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Scope;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Component
@Scope("prototype")
// this class is existed temporarily
public class ClientHandler extends SimpleChannelInboundHandler<ImmutablePacket> {



	@Autowired
	Logger log;

	
	@Autowired
	StateMachine<MachineState, IMachineEvent> machine;


	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		machine.sendEvent(ClientFeed.CLIENT_CONNECTED);
		super.channelActive(ctx);
	}

	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ImmutablePacket packet) throws Exception {

		
		
		//int opcode = packet.getOpcode();

		//switch (opcode) {

		//case 0x9000:
		//	machine.sendEvent(ClientFeed.CONNECTION_ACCEPTED);
			//break;
	//	}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

		log.info("Client Connection Error :{}", cause.getMessage());
	}

}
