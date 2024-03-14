package org.sokybot.machine.network;

import java.util.Set;

import org.slf4j.Logger;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.GlobalOpcode;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Sharable
@Component
@Lazy
public class ClientServerBridge extends SimpleChannelInboundHandler<ImmutablePacket> {

	private ChannelHandlerContext clientCtx;
	private ChannelHandlerContext serverCtx;

	private final Set<Integer> preventedServerOpcodes;
	private final Set<Integer> preventedClientOpcode;

	@Autowired
	Logger log;

	public ClientServerBridge() {

		this.preventedClientOpcode = Set.of(GlobalOpcode.HANDSHAKE_ACCEPTANCE, GlobalOpcode.MODULE_IDENTIFICATION,
				GlobalOpcode.HANDSHAKE, ClientOpcode.AUTH_REQUEST, ClientOpcode.JOIN_REQUEST);
		this.preventedServerOpcodes = Set.of(
				// 0x5000
				// , 0x9000
				// ,0x2001
				// 0x6100
				// ,0x2002
				0xA102
		// , 0x6101

		);

	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {

		NetworkPeer peer = ctx.channel().attr(NetworkAttributes.TRANSPORT).get();
		if (peer == NetworkPeer.CLIENT) {
			this.clientCtx = ctx;
			log.info("Client channel is now active");
		} else if (peer == NetworkPeer.SERVER) {
			this.serverCtx = ctx;
			log.info("Server channel is now active");
		}
		super.channelActive(ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ImmutablePacket msg) throws Exception {
		NetworkPeer peer = ctx.channel().attr(NetworkAttributes.TRANSPORT).get();
		int opcode = msg.getOpcode() ; 
		boolean blocked = (peer == NetworkPeer.CLIENT && this.preventedClientOpcode.contains(opcode ))
				||(peer == NetworkPeer.SERVER &&  this.preventedServerOpcodes.contains(opcode)) ; 
		
		if (msg.getOpcode() != 0x2002) {
			
			//log.info("{} Send [{}] {} ", (peer == NetworkPeer.CLIENT) ? "Client" : "Server",
				//	((blocked) ? "Blocked" : "Allowed"), msg);
		
		}
		MutablePacket packet = MutablePacket.wrap(msg.toBytes());
		packet.setPacketSource(peer);
		if (peer == NetworkPeer.CLIENT) {

			if (!this.preventedClientOpcode.contains(opcode)) {
				this.serverCtx.writeAndFlush(packet);
			}

		} else {
			if (!this.preventedServerOpcodes.contains(opcode)) {
				this.clientCtx.writeAndFlush(packet);
			}
		}

		ctx.fireChannelRead(msg);

	}
}
