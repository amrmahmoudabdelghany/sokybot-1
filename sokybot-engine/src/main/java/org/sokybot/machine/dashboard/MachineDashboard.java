package org.sokybot.machine.dashboard;

import java.awt.BorderLayout;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.annotation.Scope;
// import org.springframework.stereotype.Component;

// @Component
// @Scope("prototype")
public class MachineDashboard  extends JPanel{

	
	// @Autowired
	private MachineControll controll ; 
	
	
	// @Autowired
	private TrainerInfoPanel charInfoPanel ; 
	
	
	
	// @PostConstruct
	private void init() { 
		
		this.controll.setBorder(BorderFactory.createEtchedBorder());
		setLayout(new BorderLayout());
		JPanel charInfo = new JPanel(new BorderLayout()) ; 
		charInfo.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
		charInfo.add(this.charInfoPanel , BorderLayout.CENTER) ; 
		add(charInfo , BorderLayout.NORTH) ;
		add(this.controll  , BorderLayout.PAGE_END) ; 
		 
	}
	
	
	
	
	
	
	
	
	
}
