package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.sokybot.devshell.util.HexUtils;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;

@Service
@Command(scope = "dev", name = "packet-send", description = "Build and send a packet by opcode and hex payload")
public class PacketSendCommand extends DevCommand {

    @Argument(index = 0, name = "group", description = "Group name", required = true)
    private String groupName;

    @Argument(index = 1, name = "name", description = "Bot name", required = true)
    private String botName;

    @Argument(index = 2, name = "opcode", description = "Packet opcode (hex, e.g. 0x6102 or 6102)", required = true)
    private String opcodeStr;

    @Argument(index = 3, name = "payload", description = "Hex payload data (no header, just data bytes)", required = false)
    private String payloadHex;

    @Option(name = "-c", aliases = {"--client"}, description = "Send to client instead of server")
    private boolean toClient = false;

    @Option(name = "-p", aliases = {"--plain"}, description = "Use plain encoding instead of encrypted")
    private boolean plainEncoding = false;

    @Override
    public Object execute() throws Exception {
        sokybotContext.findGroupCtx(groupName).ifPresentOrElse(group -> {
            group.findMachineCtx(botName).ifPresentOrElse(bot -> {
                if (!bot.getProxyConnection().isConnected()) {
                    error("Bot '%s' is not connected.", botName);
                    return;
                }

                try {
                    int opcode = HexUtils.parseOpcode(opcodeStr);
                    byte[] data = (payloadHex != null && !payloadHex.isEmpty())
                            ? HexUtils.hexToBytes(payloadHex)
                            : new byte[0];

                    Encoding enc = plainEncoding ? Encoding.PLAIN : Encoding.ENCRYPTED;

                    var builder = MutablePacket.getBuilder(data.length, opcode)
                            .packetEncoding(enc)
                            .dataEncoding(Encoding.PLAIN)
                            .packetSource(NetworkPeer.BOT);

                    if (data.length > 0) {
                        builder.putBytes(data);
                    }

                    MutablePacket packet = builder.build();

                    if (toClient) {
                        bot.getProxyConnection().sendToClient(packet);
                        println("Sent to client: opcode=0x%04X, %d data bytes", opcode, data.length);
                    } else {
                        bot.getProxyConnection().sendToServer(packet);
                        println("Sent to server: opcode=0x%04X, %d data bytes", opcode, data.length);
                    }
                } catch (Exception e) {
                    error("Failed to send packet: %s", e.getMessage());
                }
            }, () -> error("Bot '%s' not found in group '%s'.", botName, groupName));
        }, () -> error("Group '%s' not found.", groupName));
        return null;
    }
}
