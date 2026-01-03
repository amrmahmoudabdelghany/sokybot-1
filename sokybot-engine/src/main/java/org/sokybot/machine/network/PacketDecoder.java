package org.sokybot.machine.network ; 

import java.util.Arrays;
import java.util.List;

import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.security.IBlowfish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
public class PacketDecoder extends ByteToMessageDecoder {
	
	
	@Autowired
	private IBlowfish blowfish ; 
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

		if (in.readableBytes() < 2) {
			return;
		}

		in.markReaderIndex();
		byte[] sizeBuffer = new byte[2];
	    in.readBytes(sizeBuffer);
	 	 
		short size = (short) (((sizeBuffer[1] & 0xff) << 8) | (sizeBuffer[0] & 0xff)); // reverse packet size to big
																						// indian
		boolean encrepted = false ; 
		if ((size & 0x8000) == 0x8000) {
			encrepted = true ; 
			log.info("Detect encrepted packet");

			size &= 0x7fff; // datapayload size
			size += 4; // 6 bytes for packet header
			size += ((size % 8) == 0) ? 0 : 8 - (size % 8); // padding
			size += 2;

		} else {
			size += 6;
		}

		if (in.readableBytes() < (size - 2)) {
			in.resetReaderIndex();
			log.info("No sufficient data into buffer expected {} but it was {} ", size, in.readableBytes());
			return;
		}

		byte[] buffer = new byte[size];
		in.readBytes(buffer, 2, buffer.length - 2);
		
		buffer[0] = sizeBuffer[0];
		buffer[1] = sizeBuffer[1];

		if(encrepted) { 
			log.info("Decoding encrepted packet");
			// Packet Size field must keep 0x80 flag to correctly generate crc byte
			 
			// and we must remove padding if exists  to correctly generate crc byte
			
			buffer = this.blowfish.decode(2 , buffer) ; 
			
			size = (short) (((buffer[1] & 0x7f) << 8) | (buffer[0] & 0xff)); // to get actual size of payload
			size += 6 ; // adding header size 
			
			if(size < buffer.length) {  // check if there exists padding 
				buffer = Arrays.copyOf(buffer, size) ; 
			}
			
			
			encrepted = false ; 
		}
		
		
		
		out.add(ImmutablePacket.wrap(buffer,Encoding.PLAIN ,  ctx.channel().attr(NetworkAttributes.TRANSPORT).get()));
		
	}

}
