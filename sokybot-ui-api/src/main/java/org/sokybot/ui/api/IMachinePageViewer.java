package org.sokybot.ui.api;

import javax.swing.JComponent;

public interface IMachinePageViewer extends IPageViewer{

	
	
	public void registerDashboard(String name , JComponent content) ;  

	public void removeDashboard(String name) ; 
}
