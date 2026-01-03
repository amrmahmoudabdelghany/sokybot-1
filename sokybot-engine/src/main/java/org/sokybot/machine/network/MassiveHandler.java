package org.sokybot.machine.network ; 


import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.ImmutablePacket;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import io.netty.buffer.ByteBuf;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Component
@Scope("prototype")
@Slf4j
public class MassiveHandler extends SimpleChannelInboundHandler<ImmutablePacket> {

	private int opcode;
	private short count;
	private ByteBuf dataBuffer;

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ImmutablePacket packet) throws Exception {

		
		if (packet.getOpcode() == 0x600D) {
	
			
			ByteBuf buffer = Unpooled.wrappedBuffer(packet.getPacketReader().readFully()) ; 
			
			
			if(buffer.readByte() == 0x01) { 
				
				count = buffer.readShortLE() ; 
				opcode = buffer.readShortLE() ; 
				this.dataBuffer = Unpooled.buffer() ; 
				
				ByteBuf header = Unpooled.wrappedBuffer(new byte[6]) ;  
				header.setShortLE(2, opcode) ; 
				this.dataBuffer.writeBytes(header) ; 
				
				
			}else { 
			 this.dataBuffer.writeBytes(buffer ) ;
				count -- ; 
			}
			
			if(count == 0) { 
				
				int packetLen = this.dataBuffer.readableBytes() ; 
				
				this.dataBuffer.setShortLE(0, packetLen - 6) ; 
				
				byte [] dataArr = new byte[packetLen];
				this.dataBuffer.readBytes(dataArr) ; 
				
				log.debug("Massive Packet [{}] " , ByteBufUtil.hexDump(dataArr));
				
				this.dataBuffer = null ; 
				this.opcode = 0 ; 
				
			  ctx.fireChannelRead(ImmutablePacket.wrap(dataArr, Encoding.PLAIN, ctx.channel().attr(NetworkAttributes.TRANSPORT).get()));
			}
		}else { 

			ctx.fireChannelRead(packet);
		}

	}
}
