package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "dev", name = "bot-create", description = "Create a new bot in a group")
public class BotCreateCommand extends DevCommand {

    @Argument(index = 0, name = "group", description = "Group name", required = true, multiValued = false)
    private String groupName;

    @Argument(index = 1, name = "name", description = "Bot name", required = true, multiValued = false)
    private String botName;

    @Override
    public Object execute() throws Exception {
        sokybotContext.findGroupCtx(groupName).ifPresentOrElse(group -> {
            println("Creating bot '%s' in group '%s'...", botName, groupName);
            try {
                group.installMachine(botName);
                println("Bot created successfully.");
            } catch (Exception e) {
                error("Failed to create bot: %s", e.getMessage());
            }
        }, () -> error("Group '%s' not found.", groupName));
        return null;
    }
}
