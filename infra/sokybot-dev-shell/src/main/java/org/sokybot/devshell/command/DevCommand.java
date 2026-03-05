package org.sokybot.devshell.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.sokybot.runtime.ISokybotContext;

public abstract class DevCommand implements Action {

    @Reference
    protected ISokybotContext sokybotContext;

    @Reference
    protected org.sokybot.settings.api.ISettingsRegistry settingsRegistry;

    protected void println(String format, Object... args) {
        System.out.println(String.format(format, args));
    }

    protected void error(String format, Object... args) {
        System.err.println(String.format("Error: " + format, args));
    }
}
