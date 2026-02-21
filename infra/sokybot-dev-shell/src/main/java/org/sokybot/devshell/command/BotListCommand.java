package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.sokybot.runtime.IMachineContext;

@Service
@Command(scope = "dev", name = "bot-list", description = "List all bots in a group")
public class BotListCommand extends DevCommand {

    @Argument(index = 0, name = "group", description = "Group name", required = true, multiValued = false)
    private String groupName;

    @Override
    public Object execute() throws Exception {
        sokybotContext.findGroupCtx(groupName).ifPresentOrElse(group -> {
            IMachineContext[] machines = group.getMachines();
            println("Bots in group '%s': %d", groupName, machines.length);
            println("%-20s %-10s", "Bot Name", "State");
            println("--------------------------------");
            for (IMachineContext bot : machines) {
                String state = bot.isRunning() ? "RUNNING" : "STOPPED";
                println("%-20s %-10s", bot.getMachineName(), state);
            }
        }, () -> error("Group '%s' not found.", groupName));
        return null;
    }
}
