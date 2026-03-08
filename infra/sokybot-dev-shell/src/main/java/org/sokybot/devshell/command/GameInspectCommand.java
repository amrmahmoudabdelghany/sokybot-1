package org.sokybot.devshell.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.sokybot.engine.api.scripting.IScriptEngine;
import org.sokybot.gamemodel.IGameModel;

/**
 * Karaf shell command that delegates game-model inspection to
 * {@code scripts/dev/GameInspect.groovy}. The Groovy script handles
 * all formatting and entity-type dispatch.
 */
@Service
@Command(scope = "dev", name = "game-inspect",
        description = "Inspect game model entities (trainer, monsters, items, spawns)")
public class GameInspectCommand extends DevCommand {

    private static final String SCRIPT_PATH = "scripts/dev/GameInspect.groovy";

    @Reference
    private IScriptEngine scriptEngine;

    @Argument(index = 0, name = "group", description = "Group name", required = true)
    private String groupName;

    @Argument(index = 1, name = "name", description = "Bot name", required = true)
    private String botName;

    @Argument(index = 2, name = "entity",
            description = "Entity type: trainer, monsters, items, spawns, or numeric ID",
            required = false)
    private String entityType;

    @Override
    public Object execute() throws Exception {
        sokybotContext.findGroupCtx(groupName).ifPresentOrElse(group -> {
            group.findMachineCtx(botName).ifPresentOrElse(bot -> {
                IGameModel model = bot.getGameModel();
                if (model == null) {
                    error("Game model not initialized for bot '%s'.", botName);
                    return;
                }

                try {
                    Path scriptFile = Paths.get(SCRIPT_PATH);
                    if (!Files.exists(scriptFile)) {
                        error("Inspection script not found: %s", SCRIPT_PATH);
                        return;
                    }

                    String script = Files.readString(scriptFile);
                    Map<String, Object> ctx = new HashMap<>();
                    ctx.put("model", model);
                    ctx.put("entityType", entityType != null ? entityType : "trainer");

                    Object result = scriptEngine.execute(script, ctx);
                    if (result != null) {
                        System.out.print(result.toString());
                    }
                } catch (Exception e) {
                    error("Script execution failed: %s", e.getMessage());
                }
            }, () -> error("Bot '%s' not found in group '%s'.", botName, groupName));
        }, () -> error("Group '%s' not found.", groupName));
        return null;
    }
}
