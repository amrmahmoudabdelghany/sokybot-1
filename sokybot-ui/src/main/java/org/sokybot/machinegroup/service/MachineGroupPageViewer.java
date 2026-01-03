package org.sokybot.machinegroup.service;

import javax.annotation.PostConstruct;
import javax.swing.Icon;
import javax.swing.JComponent;

import org.sokybot.IPageViewer;
import org.sokybot.app.AppConstants;
import org.sokybot.machinegroup.PageContainer;
import org.sokybot.machinegroup.navigationtree.INavTree;
import org.sokybot.machinegroup.navigationtree.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.icons.FlatSearchIcon;



@Component
public class MachineGroupPageViewer implements IPageViewer {

	
	@Autowired
	private INavTree navTree;

	@Autowired
	private PageContainer pageContainer;

	
	@Value("${" + AppConstants.GROUP_NAME + "}")
	private String groupName ; 

	
	@PostConstruct
	private void test() { 
		System.out.println("MachinePageViewer :: GroupName :: " + this.groupName) ; 
		
	}
	@Override
	public void registerPage(String name, Icon icon, JComponent content) {
	  
		
		String path = this.groupName + "." + name ; 
		System.out.println("MachineGroupPageViewer :: Try to insert tree node at : " + path) ; 
		System.out.println("MachineGroupPageViewer :: Where MachineGroup is : " + this.groupName ) ; 
		navTree.putNode(this.groupName, TreeNode.makeTreeNode(name, new FlatSearchIcon()));
		this.pageContainer.addPage(path, content);
		
	}
	
	@Override
	public void registerPage(String parent, String name, Icon icon, JComponent content) {
	 // check if parent exists
		registerPage(parent + "." + name , icon , content) ; 
	}
	
	@Override
	public void removePage(String pageName) {
 
		//TODO implement this method
		
	}
	
}
