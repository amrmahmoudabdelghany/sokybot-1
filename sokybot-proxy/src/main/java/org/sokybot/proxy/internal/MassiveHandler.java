package org.sokybot.proxy.internal;

import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.ImmutablePacket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Handles reassembly of massive (fragmented) packets.
 * Massive packets use opcode 0x600D.
 */
public class MassiveHandler extends SimpleChannelInboundHandler<ImmutablePacket> {
    
    private static final int MASSIVE_OPCODE = 0x600D;
    
    private int opcode;
    private short count;
    private ByteBuf dataBuffer;
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImmutablePacket packet) throws Exception {
        if (packet.getOpcode() == MASSIVE_OPCODE) {
            ByteBuf buffer = Unpooled.wrappedBuffer(packet.getPacketReader().readFully());
            
            if (buffer.readByte() == 0x01) {
                // First fragment
                count = buffer.readShortLE();
                opcode = buffer.readShortLE();
                dataBuffer = Unpooled.buffer();
                
                ByteBuf header = Unpooled.wrappedBuffer(new byte[6]);
                header.setShortLE(2, opcode);
                dataBuffer.writeBytes(header);
            } else {
                // Subsequent fragment
                dataBuffer.writeBytes(buffer);
                count--;
            }
            
            if (count == 0) {
                // All fragments received
                int packetLen = dataBuffer.readableBytes();
                dataBuffer.setShortLE(0, packetLen - 6);
                
                byte[] dataArr = new byte[packetLen];
                dataBuffer.readBytes(dataArr);
                
                dataBuffer = null;
                opcode = 0;
                
                ctx.fireChannelRead(ImmutablePacket.wrap(
                    dataArr, 
                    Encoding.PLAIN, 
                    ctx.channel().attr(NetworkAttributes.TRANSPORT).get()
                ));
            }
        } else {
            ctx.fireChannelRead(packet);
        }
    }
}
