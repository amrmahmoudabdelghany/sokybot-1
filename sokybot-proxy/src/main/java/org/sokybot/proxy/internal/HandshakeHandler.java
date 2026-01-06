package org.sokybot.proxy.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.IPacketReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.proxy.IConnectionListener;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;

/**
 * Handles the security handshake protocol between proxy and game server.
 * This includes Blowfish key exchange, CRC/Count setup, and challenge/response.
 */
public class HandshakeHandler {
    
    private static final int SETUP_OPCODE = 0x5000;
    private static final int ACCEPTANCE_OPCODE = 0x9000;
    private static final int CHALLENGE_OPCODE = 0x5001;
    
    private final NetworkComponents networkComponents;
    private final IConnectionListener listener;
    private final Channel serverChannel;
    
    private int clientSecret;
    private int serverSecret;
    private int sharedSecret;
    private final int RND = 0x33;
    
    private byte[] finalKey = null;
    private ByteBuffer privateKey;
    
    private boolean clientlessMode;
    
    public HandshakeHandler(NetworkComponents networkComponents, 
                           IConnectionListener listener,
                           Channel serverChannel,
                           boolean clientlessMode) {
        this.networkComponents = networkComponents;
        this.listener = listener;
        this.serverChannel = serverChannel;
        this.clientlessMode = clientlessMode;
    }
    
    /**
     * Handles the setup packet from server (opcode 0x2001).
     * Configures security and sends authentication if in clientless mode.
     */
    public void handleSetupPacket(ImmutablePacket setupPacket) {
        System.out.println("Sokybot Proxy: Setting up security protocol");
        
        IPacketReader reader = setupPacket.getPacketReader();
        int countSeed = reader.readInt(9);
        int crcSeed = reader.readInt(13);
        int finalKeySeed1 = reader.readInt(17);
        int finalKeySeed2 = reader.readInt(21);
        int primitiveRoot = reader.readInt(25);
        int primaryNumber = reader.readInt(29);
        this.serverSecret = reader.readInt(33);
        
        // Configure CRC and Count security
        networkComponents.getCrcSecurity().configur(crcSeed);
        networkComponents.getCountSecurity().configur(countSeed);
        
        // Generate secrets
        this.clientSecret = (int) generateSecrets(primitiveRoot, RND, primaryNumber);
        this.sharedSecret = (int) generateSecrets(serverSecret, RND, primaryNumber);
        
        // Set Final Key
        ByteBuffer finalKeySeeds = ByteBuffer.allocate(8);
        finalKeySeeds.order(ByteOrder.LITTLE_ENDIAN);
        finalKeySeeds.putInt(finalKeySeed1);
        finalKeySeeds.putInt(finalKeySeed2);
        
        ByteBuffer sharedSecretBuffer = ByteBuffer.allocate(4);
        sharedSecretBuffer.order(ByteOrder.LITTLE_ENDIAN);
        sharedSecretBuffer.putInt(sharedSecret);
        
        transformValue(finalKeySeeds.array(), sharedSecretBuffer.array(), (byte) 0x3);
        this.finalKey = finalKeySeeds.array();
        
        if (clientlessMode) {
            authenticateAsBot(primitiveRoot, primaryNumber, sharedSecretBuffer);
        }
        
        networkComponents.getBlowfish().configur(this.finalKey);
        
        if (listener != null) {
            listener.onSecuritySetupComplete();
        }
    }
    
    private void authenticateAsBot(int primitiveRoot, int primaryNumber, ByteBuffer sharedSecretBuffer) {
        System.out.println("Sokybot Proxy: Authenticating as clientless bot");
        
        // Generate Private Key
        byte PK_KeyByte = (byte) (this.sharedSecret & 0x03);
        this.privateKey = ByteBuffer.allocate(8);
        this.privateKey.order(ByteOrder.LITTLE_ENDIAN);
        this.privateKey.putInt(this.serverSecret);
        this.privateKey.putInt(this.clientSecret);
        
        transformValue(privateKey.array(), sharedSecretBuffer.array(), PK_KeyByte);
        networkComponents.getBlowfish().configur(privateKey.array());
        
        // Generate Private Key Data
        byte PD_KeyByte = (byte) (this.clientSecret & 0x07);
        ByteBuffer privateData = ByteBuffer.allocate(8);
        privateData.order(ByteOrder.LITTLE_ENDIAN);
        privateData.putInt(this.clientSecret);
        privateData.putInt(this.serverSecret);
        
        transformValue(privateData.array(), sharedSecretBuffer.array(), PD_KeyByte);
        networkComponents.getBlowfish().encode(0, privateData.array());
        
        // Reset secrets
        this.clientSecret = (int) generateSecrets(primitiveRoot, RND, primaryNumber);
        this.sharedSecret = (int) generateSecrets(this.serverSecret, RND, primaryNumber);
        
        // Build auth packet
        ByteBuffer authPacket = ByteBuffer.allocate(12);
        authPacket.order(ByteOrder.LITTLE_ENDIAN);
        authPacket.putInt(clientSecret);
        authPacket.put(privateData.array());
        
        MutablePacket packet = MutablePacket.getBuilder(12, SETUP_OPCODE)
                .putBytes(authPacket.array())
                .build();
        
        System.out.println("Sokybot Proxy: Sending auth packet " + packet);
        serverChannel.writeAndFlush(packet);
    }
    
    /**
     * Handles the challenge packet from server (opcode 0x5001).
     * Validates and responds with acceptance or rejection.
     */
    public void handleChallengePacket(ImmutablePacket challengePacket) {
        if (!clientlessMode) {
            return;
        }
        
        System.out.println("Sokybot Proxy: Handling server challenge");
        
        IPacketReader reader = challengePacket.getPacketReader();
        ByteBuffer actualPrivateData = ByteBuffer.wrap(reader.readBytes(1, 8));
        
        ByteBuffer expectedPrivateData = ByteBuffer.allocate(8);
        expectedPrivateData.order(ByteOrder.LITTLE_ENDIAN);
        expectedPrivateData.putInt(this.serverSecret);
        expectedPrivateData.putInt(this.clientSecret);
        
        byte PD_KeyByte = (byte) (this.serverSecret & 0x07);
        
        ByteBuffer sharedSecretBuffer = ByteBuffer.allocate(4);
        sharedSecretBuffer.order(ByteOrder.LITTLE_ENDIAN);
        sharedSecretBuffer.putInt(sharedSecret);
        
        transformValue(expectedPrivateData.array(), sharedSecretBuffer.array(), PD_KeyByte);
        
        networkComponents.getBlowfish().configur(this.privateKey.array());
        byte[] encoded = networkComponents.getBlowfish().encode(0, expectedPrivateData.array());
        networkComponents.getBlowfish().configur(this.finalKey);
        
        expectedPrivateData = ByteBuffer.wrap(encoded);
        
        if (expectedPrivateData.equals(actualPrivateData)) {
            // Send acceptance
            MutablePacket acceptancePacket = MutablePacket.wrap(new byte[6]);
            acceptancePacket.setPacketSize((short) 0x00);
            acceptancePacket.setOpcode((short) ACCEPTANCE_OPCODE);
            acceptancePacket.setPacketEncoding(Encoding.PLAIN);
            acceptancePacket.setDataEncoding(Encoding.PLAIN);
            acceptancePacket.setPacketSource(NetworkPeer.BOT);
            
            serverChannel.writeAndFlush(acceptancePacket);
            
            System.out.println("Sokybot Proxy: Handshake accepted, Final Key: " + ByteBufUtil.hexDump(this.finalKey));
            
            if (listener != null) {
                listener.onHandshakeComplete();
            }
            
            if (clientlessMode) {
                sendIdentification();
            }
        } else {
            System.err.println("Sokybot Proxy: Handshake failed - challenge mismatch");
            
            if (listener != null) {
                listener.onHandshakeFailed("Challenge validation failed");
            }
        }
    }
    
    private void sendIdentification() {
        System.out.println("Sokybot Proxy: Sending client identification");
        String moduleName = "SR_Client";
        int packetSize = moduleName.length() + 3;

        MutablePacket moduleIdentification = MutablePacket.getBuilder(packetSize, 0x2001)
                .packetEncoding(Encoding.ENCRYPTED)
                .putShort((short) moduleName.length())
                .putBytes(moduleName.getBytes())
                .put((byte) 0x00)
                .build();
        
        serverChannel.writeAndFlush(moduleIdentification);
    }
    
    /**
     * Handles the server identification packet (opcode 0x2001).
     * Extracts the service name and notifies the listener.
     */
    public void handleServerIdentification(ImmutablePacket packet) {
        IPacketReader reader = packet.getPacketReader();
        int serviceNameLen = reader.readShort(0);
        String name = new String(reader.readBytes(2, serviceNameLen));
        
        System.out.println("Sokybot Proxy: Server identified as " + name);
        
        if (listener != null) {
            listener.onServerIdentified(name);
        }
    }
    
    private long generateSecrets(long g, int x, long p) {
        long result = 1;
        long mult = g;
        if (x == 0) return 0;
        
        while (x > 0) {
            int y = x & 1;
            if (1 == y) {
                result = (mult * result) % p;
            }
            x = x >> 1;
            mult = (mult * mult) % p;
        }
        return result;
    }
    
    private void transformValue(byte[] stream, byte[] Dkey, byte keyByte) {
        stream[0] ^= (byte) (stream[0] + Dkey[0] + keyByte);
        stream[1] ^= (byte) (stream[1] + Dkey[1] + keyByte);
        stream[2] ^= (byte) (stream[2] + Dkey[2] + keyByte);
        stream[3] ^= (byte) (stream[3] + Dkey[3] + keyByte);
        
        stream[4] ^= (byte) (stream[4] + Dkey[0] + keyByte);
        stream[5] ^= (byte) (stream[5] + Dkey[1] + keyByte);
        stream[6] ^= (byte) (stream[6] + Dkey[2] + keyByte);
        stream[7] ^= (byte) (stream[7] + Dkey[3] + keyByte);
    }
}
