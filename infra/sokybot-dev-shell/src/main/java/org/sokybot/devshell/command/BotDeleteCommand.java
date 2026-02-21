package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "dev", name = "bot-delete", description = "Delete a bot from a group")
public class BotDeleteCommand extends DevCommand {

    @Argument(index = 0, name = "group", description = "Group name", required = true, multiValued = false)
    private String groupName;

    @Argument(index = 1, name = "name", description = "Bot name", required = true, multiValued = false)
    private String botName;

    @Override
    public Object execute() throws Exception {
        sokybotContext.findGroupCtx(groupName).ifPresentOrElse(group -> {
            println("Deleting bot '%s' from group '%s'...", botName, groupName);
            try {
                group.removeMachine(botName);
                println("Bot deleted successfully.");
            } catch (Exception e) {
                error("Failed to delete bot: %s", e.getMessage());
            }
        }, () -> error("Group '%s' not found.", groupName));
        return null;
    }
}
