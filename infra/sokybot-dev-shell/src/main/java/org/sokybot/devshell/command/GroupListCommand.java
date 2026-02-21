package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.sokybot.runtime.IGroupContext;

@Service
@Command(scope = "dev", name = "group-list", description = "List all bot groups")
public class GroupListCommand extends DevCommand {

    @Override
    public Object execute() throws Exception {
        IGroupContext[] groups = sokybotContext.getGroups();
        
        println("%-20s %-60s", "Group Name", "Game Path");
        println("--------------------------------------------------------------------------------");
        
        for (IGroupContext group : groups) {
            println("%-20s %-60s", group.name(), group.getGamePath());
        }
        
        return null;
    }
}
