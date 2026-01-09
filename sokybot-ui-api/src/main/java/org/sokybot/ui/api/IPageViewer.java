package org.sokybot.ui.api;

import javax.swing.Icon;
import javax.swing.JComponent;

public interface IPageViewer {
	
	public void registerPage(String parent , String name , Icon icon , JComponent content ) ; 
	public void registerPage(String name , Icon icon , JComponent content ) ; 
	
	public void removePage(String pageName) ; 
	
	
}
