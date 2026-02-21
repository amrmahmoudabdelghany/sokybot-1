package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.sokybot.engine.plugin.IActuatorLoader;
import org.sokybot.engine.plugin.IScriptActuatorLoader;
import org.sokybot.engine.plugin.LoadedActuator;
import org.sokybot.machinepages.ScriptPageLoader;

import java.util.List;

@Service
@Command(scope = "dev", name = "script-list", description = "List all available scripts (Actuators & Pages)")
public class ScriptListCommand extends DevCommand {

    @Reference
    private IActuatorLoader actuatorLoader;

    @Reference
    private IScriptActuatorLoader scriptActuatorLoader;

    @Reference
    private ScriptPageLoader pageLoader;

    @Override
    public Object execute() throws Exception {
        println("Scripted Actuators (Monitoring: %s):", scriptActuatorLoader.getScriptsDirectory());
        println("--------------------------------------------------------------------------------");
        List<LoadedActuator> actuators = actuatorLoader.getLoadedActuators();
        for (LoadedActuator loaded : actuators) {
            if (loaded.getSourcePath() != null && loaded.getSourcePath().toString().contains("actuators")) {
                 println("- %-20s %s", loaded.getName(), loaded.getSourcePath().getFileName());
            }
        }
        
        println("\nScripted UI Pages (Monitoring: scripts/pages):");
        println("--------------------------------------------");
        for (String page : pageLoader.getAvailablePages()) {
            println("- %s", page);
        }
        
        return null;
    }
}
