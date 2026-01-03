/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sokybot.app.machinebuilder;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.annotation.PostConstruct;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXBusyLabel;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatComboBox;
import com.formdev.flatlaf.extras.components.FlatPasswordField;
import com.formdev.flatlaf.extras.components.FlatTextField;


/**
 *
 * @author AMROO
 */

@Component
@Scope("prototype")
public class AccountInfoPanel extends JPanel {

	
	public static final String AGENT_SERVER_PROP = "agent_server" ; 
	
	private FlatTextField username;
	private FlatPasswordField password;
	private FlatPasswordField passcode;
	private FlatTextField agentServer ; 
	//private FlatComboBox<String> agentServer;
	//private FlatTextField charName;
	private ImageIcon loadingIcon;

	private JXBusyLabel iconCont;

	public AccountInfoPanel() {
		//loadingIcon = new ImageIcon(getClass().getClassLoader().getResource("7.gif"));
		this.username = new FlatTextField();
		this.password = new FlatPasswordField();
		this.passcode = new FlatPasswordField();
		
		//this.agentServer = new FlatComboBox<String>();
		this.agentServer = new FlatTextField() ; 
		this.iconCont = new JXBusyLabel();

	//	this.charName = new FlatTextField();
		

	}

	public void setImgVisible() {
		if (this.iconCont != null)
			this.iconCont.setVisible(false);
	}

	@PostConstruct
	 void init() {


		this.username.setPlaceholderText("Username");
		this.password.setPlaceholderText("Password");
		this.passcode.setPlaceholderText("Passcode");
		//this.charName.setPlaceholderText("CharacterEntity Name");
		this.agentServer.setPlaceholderText("Server");
		
		this.password.putClientProperty( FlatClientProperties.STYLE, "showRevealButton: true" );
		this.passcode.putClientProperty( FlatClientProperties.STYLE, "showRevealButton: true" );
		this.username.setLeadingIcon(new FlatSVGIcon("icons/user.svg"));
		this.password.setLeadingIcon(new FlatSVGIcon("icons/key.svg"));
		this.passcode.setLeadingIcon(new FlatSVGIcon("icons/key.svg"));
		this.agentServer.setLeadingIcon(new FlatSVGIcon("icons/lan.svg")) ; 
		
		this.username.setPreferredSize(new Dimension(190, 20));
		this.password.setPreferredSize(new Dimension(190, 20));
		this.passcode.setPreferredSize(new Dimension(190, 20));
		//this.charName.setPreferredSize(new Dimension(190, 20));
		this.agentServer.setPreferredSize(new Dimension(190, 20)); // 190 20
		this.iconCont.setPreferredSize(new Dimension(30, 20));
		this.iconCont.setVisible(false);
		// this.iconCont.setMinimumSize(new Dimension(30 , 20));
		BoxLayout layout  = new BoxLayout(this, BoxLayout.Y_AXIS) ; 
		
		setLayout(layout);
		
		add(this.username);
		
		add(this.password);
		
		add(this.passcode);
		Box  line = Box.createHorizontalBox() ; 
		line.add(this.agentServer) ; 
		line.add(this.iconCont) ; 
		
		add(line);
		
	//	add(this.iconCont);
		//add(this.charName);
	}

	
	
	public String getUserName() {
		return this.username.getText();
	}

	public String getPassword() {
		return this.password.getText();
	}

	public String getPasscode() {
		return this.passcode.getText();
	}

	//public String getCharName() {
	//	return this.charName.getText();
	//}

	public String getAgentServerName() {
		//return (String) this.agentServer.getSelectedItem();
		return this.agentServer.getText() ; 
	}

	public void hideLoadingImage() {
		this.iconCont.setBusy(false);
		this.iconCont.setVisible(false);
	}

	public void showLoadingImage() {
		this.iconCont.setBusy(true);
		this.iconCont.setVisible(true);
	}
	

//	public void setAgentServerNames(String[] sNames) {
//		// this.agentServer.removeAll();
//		this.agentServer.removeAllItems();
//		for (int i = 0; i < sNames.length; i++) {
//			this.agentServer.addItem((String) sNames[i]);
//		}
//	}
//	
	//public String getSelectedAgent() { 
	 // return(String)this.agentServer.getSelectedItem();
	//}

	public static void main(String args[]) {
		FlatDarkLaf.setup();
		JFrame fram = new JFrame();
		fram.setResizable(false);
		AccountInfoPanel ac = new AccountInfoPanel();
		ac.init();
		JButton btn = new JButton("hide");
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ac.hideLoadingImage();
				;
			}
		});

		JButton btnShow = new JButton("show");
		btnShow.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ac.showLoadingImage();
			}
		});
		Box panel = Box.createHorizontalBox();

		panel.add(ac);
		panel.add(btnShow);
		panel.add(btn);
		fram.add(panel);
		fram.pack();
		fram.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fram.setVisible(true);
	}


}
