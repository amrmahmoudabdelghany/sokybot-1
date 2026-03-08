package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.sokybot.devshell.util.HexUtils;
import org.sokybot.network.packet.MutablePacket;

@Service
@Command(scope = "dev", name = "inject", description = "Inject a hex packet to client or server")
public class InjectCommand extends DevCommand {

    @Argument(index = 0, name = "group", description = "Group name", required = true, multiValued = false)
    private String groupName;

    @Argument(index = 1, name = "name", description = "Bot name", required = true, multiValued = false)
    private String botName;

    @Argument(index = 2, name = "hex", description = "Hex packet data (full packet including 6-byte header)", required = true, multiValued = false)
    private String hexData;

    @Option(name = "-c", aliases = {"--client"}, description = "Inject to client (default is server)", required = false, multiValued = false)
    private boolean toClient = false;

    @Override
    public Object execute() throws Exception {
        sokybotContext.findGroupCtx(groupName).ifPresentOrElse(group -> {
            group.findMachineCtx(botName).ifPresentOrElse(bot -> {
                if (!bot.getProxyConnection().isConnected()) {
                    error("Bot '%s' is not connected.", botName);
                    return;
                }

                try {
                    byte[] data = HexUtils.hexToBytes(hexData);
                    MutablePacket packet = MutablePacket.wrap(data);

                    if (toClient) {
                        println("Injecting packet to client for bot '%s'...", botName);
                        bot.getProxyConnection().sendToClient(packet);
                    } else {
                        println("Injecting packet to server for bot '%s'...", botName);
                        bot.getProxyConnection().sendToServer(packet);
                    }
                    println("Successfully injected %d bytes.", data.length);
                } catch (Exception e) {
                    error("Injection failed: %s", e.getMessage());
                }
            }, () -> error("Bot '%s' not found in group '%s'.", botName, groupName));
        }, () -> error("Group '%s' not found.", groupName));
        return null;
    }
}
