package org.sokybot.service;

import javax.swing.*;
import java.awt.*;

public interface IMainFrameConfigurator {

    public void addExtraWindow(String id, String title , Icon icon , Component comp) ;
    public void addPage( String pageId ,Icon icon ,   String pageTitle , Component component) ;


}
