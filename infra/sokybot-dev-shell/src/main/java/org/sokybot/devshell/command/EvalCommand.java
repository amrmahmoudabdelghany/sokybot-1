package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.sokybot.engine.api.scripting.IScriptEngine;

import java.util.HashMap;
import java.util.Map;

@Service
@Command(scope = "dev", name = "eval", description = "Evaluate a Groovy script in bot context")
public class EvalCommand extends DevCommand {

    @Argument(index = 0, name = "group", description = "Group name", required = true, multiValued = false)
    private String groupName;

    @Argument(index = 1, name = "name", description = "Bot name", required = true, multiValued = false)
    private String botName;

    @Argument(index = 2, name = "script", description = "Groovy script to execute", required = true, multiValued = false)
    private String script;

    @Reference
    private IScriptEngine scriptEngine;

    @Override
    public Object execute() throws Exception {
        sokybotContext.findGroupCtx(groupName).ifPresentOrElse(group -> {
            group.findMachineCtx(botName).ifPresentOrElse(bot -> {
                println("Evaluating script for bot '%s'...", botName);
                try {
                    Map<String, Object> context = new HashMap<>();
                    context.put("bot", bot);
                    context.put("ctx", bot);
                    context.put("model", bot.getGameModel());
                    context.put("trainer", bot.getGameModel().getTrainer());
                    context.put("proxy", bot.getProxyConnection());
                    context.put("group", group);
                    
                    Object result = scriptEngine.execute(script, context);
                    println("Result: %s", result);
                } catch (Exception e) {
                    error("Evaluation failed: %s", e.getMessage());
                    e.printStackTrace();
                }
            }, () -> error("Bot '%s' not found in group '%s'.", botName, groupName));
        }, () -> error("Group '%s' not found.", groupName));
        return null;
    }
}
