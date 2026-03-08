import org.sokybot.network.NetworkPeer
import org.sokybot.network.packet.Encoding
import org.sokybot.network.packet.MutablePacket

/**
 * Shared packet utilities for dev scripting.
 * Can be loaded via: evaluate(new File("scripts/dev/PacketUtils.groovy"))
 */

static byte[] hexToBytes(String hex) {
    hex = hex.replaceAll("\\s+", "")
    int len = hex.length()
    if (len % 2 != 0) throw new IllegalArgumentException("Hex string must have an even length")
    byte[] data = new byte[len / 2]
    for (int i = 0; i < len; i += 2) {
        data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                              + Character.digit(hex.charAt(i + 1), 16))
    }
    return data
}

static String bytesToHex(byte[] bytes) {
    def sb = new StringBuilder(bytes.length * 2)
    bytes.each { b -> sb.append(String.format("%02X", b & 0xFF)) }
    return sb.toString()
}

static int parseOpcode(String s) {
    s = s.trim()
    if (s.startsWith("0x") || s.startsWith("0X")) return Integer.parseInt(s.substring(2), 16)
    try { return Integer.parseInt(s, 16) }
    catch (NumberFormatException e) { return Integer.parseInt(s) }
}

static MutablePacket buildPacket(int opcode, byte[] data,
                                  boolean encrypted = true,
                                  NetworkPeer source = NetworkPeer.BOT) {
    def enc = encrypted ? Encoding.ENCRYPTED : Encoding.PLAIN
    def builder = MutablePacket.getBuilder(data.length, opcode)
            .packetEncoding(enc)
            .dataEncoding(Encoding.PLAIN)
            .packetSource(source)
    if (data.length > 0) builder.putBytes(data)
    return builder.build()
}

static MutablePacket buildPacket(String opcodeStr, String payloadHex,
                                  boolean encrypted = true) {
    int opcode = parseOpcode(opcodeStr)
    byte[] data = (payloadHex != null && !payloadHex.isEmpty())
            ? hexToBytes(payloadHex) : new byte[0]
    return buildPacket(opcode, data, encrypted)
}
