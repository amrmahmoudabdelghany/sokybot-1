package org.sokybot.machinegroup;

import java.awt.CardLayout;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.sokybot.machinegroup.navigationtree.NavTreeSelectionEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PageContainer extends JPanel {

	
	private CardLayout cardLayout  ; 
	
	private final Map<String, java.awt.Component> comps = new HashMap<>() ; 
	

	
	@PostConstruct
	void init() { 
		this.cardLayout = new CardLayout() ; 
		
		setLayout(this.cardLayout);
		setBorder(BorderFactory.createEtchedBorder()) ; 
		
		
	}
	
	
	public void addPage(String name , java.awt.Component component) { 
		
		this.add(name  , component)  ; 
	
		
	}
	
	public void removePage(String name) { 
		if(comps.containsKey(name)) { 
			java.awt.Component comp = this.comps.get(name) ; 
			this.remove(comp);
			this.comps.remove(name);
		}
	}
	
	@EventListener
    public void showPage(NavTreeSelectionEvent navTreeSelectionEvent ) { 
    	
     String path = 	navTreeSelectionEvent.getSelectedPath() ;
     
     cardLayout.show(this, path);
    log.info("Main Page Container Recive Path {} " ,   path) ; 
    }
	
}
