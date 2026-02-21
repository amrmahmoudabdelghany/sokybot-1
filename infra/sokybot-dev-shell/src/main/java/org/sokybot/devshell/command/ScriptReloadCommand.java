package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.sokybot.engine.plugin.IScriptActuatorLoader;
import org.sokybot.machinepages.ScriptPageLoader;

@Service
@Command(scope = "dev", name = "script-reload", description = "Force reload of all scripts")
public class ScriptReloadCommand extends DevCommand {

    @Reference
    private IScriptActuatorLoader scriptActuatorLoader;

    @Reference
    private ScriptPageLoader pageLoader;

    @Override
    public Object execute() throws Exception {
        println("Reloading all scripted actuators and UI pages...");
        try {
            scriptActuatorLoader.reloadAll();
            pageLoader.scanAndLoad();
            println("Reload complete.");
        } catch (Exception e) {
            error("Failed to reload scripts: %s", e.getMessage());
        }
        return null;
    }
}
