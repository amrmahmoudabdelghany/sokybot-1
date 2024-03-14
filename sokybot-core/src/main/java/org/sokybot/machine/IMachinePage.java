package org.sokybot.machine;



import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_HIDE_TAB_AREA_WITH_ONE_TAB;

import javax.swing.Icon;
import javax.swing.JTabbedPane;



public abstract class IMachinePage extends JTabbedPane {


	public abstract String getName();

	public abstract Icon getIcon();

	
	
	public IMachinePage() { 
		
		putClientProperty(TABBED_PANE_HIDE_TAB_AREA_WITH_ONE_TAB , true) ; 
	}
	
	

}
