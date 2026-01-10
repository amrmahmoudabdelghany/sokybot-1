package org.sokybot.machineui.page;

import javax.swing.Icon;

import com.formdev.flatlaf.icons.FlatSearchIcon;

public class LogPage extends IMachinePage {

    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return "Logs";
    }

    @Override
    public Icon getIcon() {
        // TODO: specific icon
        return new FlatSearchIcon();
    }
}
