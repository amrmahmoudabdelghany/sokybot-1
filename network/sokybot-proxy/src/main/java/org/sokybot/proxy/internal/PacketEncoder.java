package org.sokybot.proxy.internal;

import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.proxy.ProxyConnection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encodes MutablePacket objects to raw bytes.
 * Handles CRC, count byte generation, and encryption.
 */
public class PacketEncoder extends MessageToByteEncoder<MutablePacket> {

    private static final Logger log = LoggerFactory.getLogger(PacketEncoder.class);

    private final NetworkComponents networkComponents;

    public PacketEncoder(NetworkComponents networkComponents) {
        this.networkComponents = networkComponents;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, MutablePacket packet, ByteBuf out) throws Exception {
        NetworkPeer peer = ctx.channel().attr(NetworkAttributes.TRANSPORT).get();

        // Throttle here (not only in an outbound handler): under OSGi, another bundle's MutablePacket
        // can fail instanceof in a handler while this encode() still receives the packet correctly.
        if (peer == NetworkPeer.SERVER && packet.getOpcode() == ClientOpcode.LOGIN_REQUEST) {
            long minIv = ProxyConnection.gatewayLoginMinIntervalMs();
            if (minIv > 0L) {
                Channel ch = ctx.channel();
                long now = System.currentTimeMillis();
                synchronized (ch) {
                    Long last = ch.attr(GatewayLoginThrottleAttributes.GATEWAY_LOGIN_6102_LAST_SENT_AT_MS).get();
                    if (last != null && now - last < minIv) {
                        if (log.isWarnEnabled()) {
                            log.warn(
                                    "Dropped duplicate gateway 0x6102 on channel {} ({} ms since last encode, min {} ms)",
                                    ch.id(),
                                    now - last,
                                    minIv);
                        }
                        throw new GatewayLoginThrottledException(
                                "Gateway 0x6102 throttled: " + (now - last) + " ms since last, min " + minIv + " ms");
                    }
                    ch.attr(GatewayLoginThrottleAttributes.GATEWAY_LOGIN_6102_LAST_SENT_AT_MS).set(now);
                }
            }
        }

        byte[] packetArr = packet.unwrap();

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
        // Blowfish may return a new array; copy wire bytes back so post-encode observers (sniffer) match TCP.
        byte[] backing = packet.unwrap();
        if (backing != null && packetArr != null && backing.length >= packetArr.length) {
            System.arraycopy(packetArr, 0, backing, 0, packetArr.length);
        }
    }
}
