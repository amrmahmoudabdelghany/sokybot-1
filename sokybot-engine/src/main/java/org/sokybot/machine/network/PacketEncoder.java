package org.sokybot.machine.network ; 

import org.slf4j.Logger;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.security.IBlowfish;
import org.sokybot.security.ICRCSecurity;
import org.sokybot.security.ICountSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@Component
@Scope("prototype")
public class PacketEncoder extends MessageToByteEncoder<MutablePacket> {

	@Autowired
	private ICountSecurity countSecurity ;
	
	@Autowired
	private ICRCSecurity crcSecurity; 
	
	@Autowired
	private IBlowfish blowfish ; 
	
	@Autowired
	private Logger log ; 
	
	@Override
	protected void encode(ChannelHandlerContext ctx, MutablePacket packet, ByteBuf out) throws Exception {
	 
		byte[] packetArr = packet.unwrap();
		
		NetworkPeer peer = 	ctx.channel().attr(NetworkAttributes.TRANSPORT).get() ; 
	 
		if(peer  == NetworkPeer.SERVER) { 
			
			packetArr[4] = this.countSecurity.generateCountByte() ; 
			packetArr[5] = 0x00 ; 
			packetArr[5] = this.crcSecurity.calculate(packetArr) ; 
				
		}
	
		if (packet.getPacketEncoding() == Encoding.ENCRYPTED)
			if (packet.getDataEncoding() == Encoding.PLAIN) {
			  packetArr = this.blowfish.encode(2, packetArr) ; 
			}
		
		
		out.writeBytes(packetArr) ; 
		
	}
}
