package org.sokybot.app.mainframe;

import javax.swing.*;

public class JSMenuBar extends JMenuBar {

    private JMenu menu = null ;

    @Override
    public void setHelpMenu(JMenu menu) {
        if(menu != null) {
            this.menu = menu;
            this.add(menu);
        }
    }

    @Override
    public JMenu getHelpMenu() {
        return this.menu ;
    }
}
