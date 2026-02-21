package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;

@Service
@Command(scope = "dev", name = "status", description = "Prints Sokybot system status")
public class StatusCommand extends DevCommand {

    @Override
    public Object execute() throws Exception {
        println("Sokybot System Status");
        println("---------------------");
        
        IGroupContext[] groups = sokybotContext.getGroups();
        println("Groups: %d", groups.length);
        
        for (IGroupContext group : groups) {
            IMachineContext[] machines = group.getMachines();
            println("  Group: %s (%d bots)", group.name(), machines.length);
            for (IMachineContext machine : machines) {
                String state = machine.isRunning() ? "RUNNING" : "STOPPED";
                println("    - %s [%s]", machine.getMachineName(), state);
            }
        }
        
        return null;
    }
}
