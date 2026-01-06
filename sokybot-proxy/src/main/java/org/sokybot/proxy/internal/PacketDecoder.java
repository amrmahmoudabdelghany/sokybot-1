package org.sokybot.proxy.internal;

import java.util.Arrays;
import java.util.List;

import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.security.IBlowfish;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * Decodes raw bytes into ImmutablePacket objects.
 * Handles encrypted packets using Blowfish.
 */
public class PacketDecoder extends ByteToMessageDecoder {
    
    private final IBlowfish blowfish;
    
    public PacketDecoder(IBlowfish blowfish) {
        this.blowfish = blowfish;
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 2) {
            return;
        }

        in.markReaderIndex();
        byte[] sizeBuffer = new byte[2];
        in.readBytes(sizeBuffer);
         
        short size = (short) (((sizeBuffer[1] & 0xff) << 8) | (sizeBuffer[0] & 0xff));
        
        boolean encrypted = false;
        if ((size & 0x8000) == 0x8000) {
            encrypted = true;
            size &= 0x7fff;
            size += 4;
            size += ((size % 8) == 0) ? 0 : 8 - (size % 8);
            size += 2;
        } else {
            size += 6;
        }

        if (in.readableBytes() < (size - 2)) {
            in.resetReaderIndex();
            return;
        }

        byte[] buffer = new byte[size];
        in.readBytes(buffer, 2, buffer.length - 2);
        
        buffer[0] = sizeBuffer[0];
        buffer[1] = sizeBuffer[1];

        if (encrypted) {
            buffer = blowfish.decode(2, buffer);
            
            size = (short) (((buffer[1] & 0x7f) << 8) | (buffer[0] & 0xff));
            size += 6;
            
            if (size < buffer.length) {
                buffer = Arrays.copyOf(buffer, size);
            }
        }
        
        NetworkPeer peer = ctx.channel().attr(NetworkAttributes.TRANSPORT).get();
        out.add(ImmutablePacket.wrap(buffer, Encoding.PLAIN, peer));
    }
}
