package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "dev", name = "bot-create", description = "Create a new bot in a group")
public class BotCreateCommand extends DevCommand {

    @Argument(index = 0, name = "group", description = "Group name", required = true, multiValued = false)
    private String groupName;

    @Argument(index = 1, name = "name", description = "Bot name", required = true, multiValued = false)
    private String botName;

    @org.apache.karaf.shell.api.action.Option(name = "--gateway", description = "Target Gateway IP", required = false)
    private String targetGateway;

    @org.apache.karaf.shell.api.action.Option(name = "--auto-login", description = "Enable auto login", required = false)
    private boolean autoLogin;

    @org.apache.karaf.shell.api.action.Option(name = "--username", description = "Login Username", required = false)
    private String username;

    @org.apache.karaf.shell.api.action.Option(name = "--password", description = "Login Password", required = false)
    private String password;

    @org.apache.karaf.shell.api.action.Option(name = "--passcode", description = "Secondary Passcode", required = false)
    private String passcode;

    @org.apache.karaf.shell.api.action.Option(name = "--server", description = "Target Game Server Name", required = false)
    private String targetAgent;

    @Override
    public Object execute() throws Exception {
        sokybotContext.findGroupCtx(groupName).ifPresentOrElse(group -> {
            println("Creating bot '%s' in group '%s'...", botName, groupName);
            try {
                group.installMachine(botName);
                println("Bot created successfully.");

                // dynamic initialization
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                boolean initialize = false;

                if (targetGateway != null) {
                    payload.put("targetGateway", targetGateway);
                    initialize = true;
                }
                if (autoLogin) {
                    payload.put("autoLogin", true);
                    payload.put("username", username);
                    payload.put("password", password);
                    payload.put("passcode", passcode);
                    payload.put("targetAgent", targetAgent);
                    initialize = true;
                }

                if (initialize && settingsRegistry != null) {
                    settingsRegistry.writeRawSettings(groupName, botName, "login", payload);
                    println("Bot initialized dynamically with CLI options.");
                }

            } catch (Exception e) {
                error("Failed to create bot: %s", e.getMessage());
            }
        }, () -> error("Group '%s' not found.", groupName));
        return null;
    }
}
