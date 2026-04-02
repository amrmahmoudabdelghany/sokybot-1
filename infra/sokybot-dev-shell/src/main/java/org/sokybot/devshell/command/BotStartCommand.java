package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.sokybot.engine.api.EngineEvent;

@Service
@Command(scope = "dev", name = "bot-start", description = "Start a bot engine")
public class BotStartCommand extends DevCommand {

    @Argument(index = 0, name = "group", description = "Group name", required = true, multiValued = false)
    private String groupName;

    @Argument(index = 1, name = "name", description = "Bot name", required = true, multiValued = false)
    private String botName;

    @Override
    public Object execute() throws Exception {
        sokybotContext.findGroupCtx(groupName).ifPresentOrElse(group -> {
            group.findMachineCtx(botName).ifPresentOrElse(bot -> {
                if (bot.isRunning()) {
                    println("Bot '%s' is already running.", botName);
                } else {
                    println("Starting bot '%s'...", botName);
                    try {
                        bot.getEngine().start();
                        bot.getEngine().sendEvent(EngineEvent.CONNECT);
                        println("Bot started.");
                    } catch (Exception e) {
                        error("Failed to start bot: %s", e.getMessage());
                    }
                }
            }, () -> error("Bot '%s' not found in group '%s'.", botName, groupName));
        }, () -> error("Group '%s' not found.", groupName));
        return null;
    }
}
