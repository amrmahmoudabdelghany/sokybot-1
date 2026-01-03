package org.sokybot.app.machinebuilder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.InputVerifier;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.extras.components.FlatTextField;

@Component
@Scope("prototype")
public class MachineDataPanel extends JPanel implements ItemListener {

	private JRadioButton rbClient, rbClientless;
	private ButtonGroup btnGroup;
	private FlatTextField txtTrainerName;
	private JLabel lblTrainerErr;
	private JCheckBox chkAutoLogin;
	private JPanel accInfoBox;

	private AccountInfoPanel accountInfoPanel;

	@Autowired
	public MachineDataPanel(AccountInfoPanel accPanel) {

		this.rbClient = new JRadioButton("Client");
		this.rbClientless = new JRadioButton("Clientless");
		this.txtTrainerName = new FlatTextField();
		this.lblTrainerErr = new JLabel();
		this.btnGroup = new ButtonGroup();
		this.chkAutoLogin = new JCheckBox("Auto Login ");
		this.accInfoBox = new JPanel(new BorderLayout());
		this.accountInfoPanel = accPanel;
	}

	@PostConstruct
	void init() {

		BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);

		setLayout(layout);

		this.chkAutoLogin.setSelected(false);
		this.chkAutoLogin.addItemListener(this);
		
		this.accountInfoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		this.lblTrainerErr.setPreferredSize(new Dimension(120 , 20));
		this.lblTrainerErr.setForeground(new Color(255 , 51 , 51));
	
		this.txtTrainerName.setInputVerifier(new InputVerifier() {
			
			@Override
			public boolean verify(JComponent input) {
				
				if(txtTrainerName.getText().isBlank()) { 
					 txtTrainerName.setOutline("error");
					lblTrainerErr.setText("This field is required ");
					return false ; 
				}else { 
					txtTrainerName.setOutline(null) ; 
					lblTrainerErr.setText(""); 
					return true ; 
				}
			}
		});

		
		this.accInfoBox.add(this.accountInfoPanel, BorderLayout.CENTER);
		this.accInfoBox.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 80));
		this.accInfoBox.setVisible(false);

		this.btnGroup.add(this.rbClient);
		this.btnGroup.add(this.rbClientless);

		this.rbClient.setSelected(true);
		this.rbClientless.setEnabled(false); // enable it when add support for clientless

		add(Box.createVerticalStrut(5));

		Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalStrut(2));
		box.add(new JLabel("Type :"));
		box.add(Box.createHorizontalStrut(5));
		box.add(this.rbClient);
		box.add(this.rbClientless);
		box.add(Box.createHorizontalGlue());

		add(box);

		add(Box.createVerticalStrut(5));

		box = Box.createHorizontalBox();
		box.add(Box.createHorizontalStrut(2));
		box.add(new JLabel("Trainer :"));
		box.add(Box.createHorizontalStrut(5));
		box.add(this.txtTrainerName);
		box.add(Box.createHorizontalStrut(5));
		box.add(this.lblTrainerErr);
		// box.add(Box.createRigidArea()) ;

		add(box);

		add(Box.createVerticalStrut(5));

		box = Box.createHorizontalBox();
		box.add(Box.createHorizontalStrut(2));
		box.add(this.chkAutoLogin);
		box.add(Box.createHorizontalGlue());

		add(box);

		add(Box.createVerticalStrut(5));

		add(this.accInfoBox);

	}

	public String getTrainerName() {
		return this.txtTrainerName.getText();
	}
	
	public void setTrainerName(String name) { 
		this.txtTrainerName.setText(name);
	}
	public void setTrainerNameEnabled(boolean enabled) { 
		this.txtTrainerName.setEnabled(enabled);
	}

	public boolean isAutoLogin() { 
		return this.chkAutoLogin.isSelected() ; 
	}
	public String getUsername() {
		return this.accountInfoPanel.getUserName() ; 
	}
	public String getPassword() { 
		return this.accountInfoPanel.getPassword() ; 
	}
	public String getPasscode() { 
		return this.accountInfoPanel.getPasscode() ; 
	}
	public String getAgentServerName() { 
		return this.accountInfoPanel.getAgentServerName() ; 
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {

		if (e.getSource() == this.chkAutoLogin) {

			if (e.getStateChange() == ItemEvent.SELECTED) {
				this.accInfoBox.setVisible(true);
			} else if (e.getStateChange() == ItemEvent.DESELECTED) {
				this.accInfoBox.setVisible(false);
			}

			((Window) this.getTopLevelAncestor()).pack();
		}

	}

	public static void main(String args[]) {
		FlatDarkLaf.setup();
		AccountInfoPanel acc = new AccountInfoPanel();
		acc.init();
		MachineDataPanel bot = new MachineDataPanel(acc);
		// bot.setPreferredSize(new Dimension(150 , 150));
		bot.init();

		JFrame frame = new JFrame();
		// frame.setPreferredSize(new Dimension(150 , 150));
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(bot, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);

	}

}
