package org.sokybot.machinegroup;

import java.awt.CardLayout;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.sokybot.machinegroup.navigationtree.NavTreeSelectionEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DashboardContainer extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private CardLayout cardLayout;

	private final Map< String , java.awt.Component> comps = new HashMap<>() ; 
	
	
	@PostConstruct
	void init() {

		this.cardLayout = new CardLayout();
		this.setLayout(this.cardLayout);
		setBorder(BorderFactory.createEtchedBorder());

	}

	public void addDashboard(String name, java.awt.Component component) {
		this.add(name, component);
		this.comps.put(name, component);
	}

	public void addPage(String name, java.awt.Component component) {

		this.add(name, component);
		this.comps.put(name, component) ; 
	}

	public void removeDashboard(String name) { 
		if(this.comps.containsKey(name)) { 
			java.awt.Component comp = this.comps.get(name) ; 
			this.remove(comp);
			this.comps.remove(name);
		}
	}
	
	@EventListener
	public void showDashboard(NavTreeSelectionEvent navTreeSelectionEvent) {

		String path = navTreeSelectionEvent.getSelectedPath();
		String pathComps[] = path.split("\\.");
		if (pathComps.length >= 2) {
			String targetDashboard = pathComps[0] + "." + pathComps[1] ; 
			cardLayout.show(this, targetDashboard);
		}
	}

}
