package org.sokybot.app.builders;

import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import org.jdesktop.swingx.JXBusyLabel;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatPasswordField;
import com.formdev.flatlaf.extras.components.FlatTextField;

public class AccountInfoPanel extends JPanel {
    private FlatTextField username;
    private FlatPasswordField password;
    private FlatPasswordField passcode;
    private FlatTextField agentServer;
    private JXBusyLabel iconCont;

    public AccountInfoPanel() {
        this.username = new FlatTextField();
        this.password = new FlatPasswordField();
        this.passcode = new FlatPasswordField();
        this.agentServer = new FlatTextField();
        this.iconCont = new JXBusyLabel();
        init();
    }

    void init() {
        this.username.setPlaceholderText("Username");
        this.password.setPlaceholderText("Password");
        this.passcode.setPlaceholderText("Passcode");
        this.agentServer.setPlaceholderText("Server");
        
        this.password.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        this.passcode.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        this.username.setLeadingIcon(new FlatSVGIcon("icons/user.svg"));
        this.password.setLeadingIcon(new FlatSVGIcon("icons/key.svg"));
        this.passcode.setLeadingIcon(new FlatSVGIcon("icons/key.svg"));
        this.agentServer.setLeadingIcon(new FlatSVGIcon("icons/lan.svg"));
        
        this.username.setPreferredSize(new Dimension(190, 20));
        this.password.setPreferredSize(new Dimension(190, 20));
        this.passcode.setPreferredSize(new Dimension(190, 20));
        this.agentServer.setPreferredSize(new Dimension(190, 20));
        this.iconCont.setPreferredSize(new Dimension(30, 20));
        this.iconCont.setVisible(false);

        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(layout);
        
        add(this.username);
        add(this.password);
        add(this.passcode);
        Box line = Box.createHorizontalBox();
        line.add(this.agentServer);
        line.add(this.iconCont);
        add(line);
    }

    public String getUserName() { return this.username.getText(); }
    public String getPassword() { return new String(this.password.getPassword()); }
    public String getPasscode() { return new String(this.passcode.getPassword()); }
    public String getAgentServerName() { return this.agentServer.getText(); }

    public void hideLoadingImage() {
        this.iconCont.setBusy(false);
        this.iconCont.setVisible(false);
    }

    public void showLoadingImage() {
        this.iconCont.setBusy(true);
        this.iconCont.setVisible(true);
    }
}
