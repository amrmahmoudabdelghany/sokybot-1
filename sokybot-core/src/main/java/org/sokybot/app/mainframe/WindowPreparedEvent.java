package org.sokybot.app.mainframe;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import javax.swing.*;


//@Component
//@Getter
//@Builder

@Getter
public class WindowPreparedEvent  extends ApplicationEvent {

    private JToolBar toolBar ;
    private JMenuBar menuBar ;
    private JFrame frame ;

    public WindowPreparedEvent(Object source, JFrame frame , JToolBar toolBar, JMenuBar menuBar) {
        super(source);
        this.toolBar = toolBar;
        this.menuBar = menuBar;
        this.frame = frame;
    }
}
