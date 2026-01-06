package org.sokybot.proxy.internal;

import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encodes MutablePacket objects to raw bytes.
 * Handles CRC, count byte generation, and encryption.
 */
public class PacketEncoder extends MessageToByteEncoder<MutablePacket> {
    
    private final NetworkComponents networkComponents;
    
    public PacketEncoder(NetworkComponents networkComponents) {
        this.networkComponents = networkComponents;
    }
    
    @Override
    protected void encode(ChannelHandlerContext ctx, MutablePacket packet, ByteBuf out) throws Exception {
        byte[] packetArr = packet.unwrap();
        
        NetworkPeer peer = ctx.channel().attr(NetworkAttributes.TRANSPORT).get();
     
        if (peer == NetworkPeer.SERVER) {
            packetArr[4] = networkComponents.getCountSecurity().generateCountByte();
            packetArr[5] = 0x00;
            packetArr[5] = networkComponents.getCrcSecurity().calculate(packetArr);
        }
    
        if (packet.getPacketEncoding() == Encoding.ENCRYPTED) {
            if (packet.getDataEncoding() == Encoding.PLAIN) {
                packetArr = networkComponents.getBlowfish().encode(2, packetArr);
            }
        }
        
        out.writeBytes(packetArr);
    }
}
