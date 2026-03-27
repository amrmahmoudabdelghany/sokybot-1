package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "dev", name = "bot-restart", description = "Restart a bot engine")
public class BotRestartCommand extends DevCommand {

    @Argument(index = 0, name = "group", description = "Group name", required = true, multiValued = false)
    private String groupName;

    @Argument(index = 1, name = "name", description = "Bot name", required = true, multiValued = false)
    private String botName;

    @Override
    public Object execute() throws Exception {
        sokybotContext.findGroupCtx(groupName).ifPresentOrElse(group -> {
            group.findMachineCtx(botName).ifPresentOrElse(bot -> {
                println("Restarting bot '%s'...", botName);
                try {
                    if (bot.isRunning()) {
                        bot.getEngine().stop();
                    }
                    bot.getEngine().start();
                    bot.getEngine().sendEvent("CONNECT");
                    println("Bot restarted.");
                } catch (Exception e) {
                    error("Failed to restart bot: %s", e.getMessage());
                }
            }, () -> error("Bot '%s' not found in group '%s'.", botName, groupName));
        }, () -> error("Group '%s' not found.", groupName));
        return null;
    }
}
