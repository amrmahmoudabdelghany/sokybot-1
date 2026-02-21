package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "dev", name = "group-create", description = "Create a new bot group")
public class GroupCreateCommand extends DevCommand {

    @Argument(index = 0, name = "name", description = "Group name", required = true, multiValued = false)
    private String name;

    @Argument(index = 1, name = "path", description = "Game path", required = true, multiValued = false)
    private String path;

    @Override
    public Object execute() throws Exception {
        println("Creating group '%s' at '%s'...", name, path);
        try {
            sokybotContext.installGroup(name, path);
            println("Group created successfully.");
        } catch (Exception e) {
            error("Failed to create group: %s", e.getMessage());
        }
        return null;
    }
}
