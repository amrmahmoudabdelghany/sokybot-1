package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "dev", name = "group-delete", description = "Delete a bot group")
public class GroupDeleteCommand extends DevCommand {

    @Argument(index = 0, name = "name", description = "Group name", required = true, multiValued = false)
    private String name;

    @Override
    public Object execute() throws Exception {
        println("Deleting group '%s'...", name);
        try {
            sokybotContext.removeGroup(name);
            println("Group deleted.");
        } catch (Exception e) {
            error("Failed to delete group: %s", e.getMessage());
        }
        return null;
    }
}
