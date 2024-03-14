package org.sokybot.machine.service;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.sokybot.IMachinePageViewer;
import org.sokybot.app.AppConstants;
import org.sokybot.machinegroup.DashboardContainer;
import org.sokybot.machinegroup.PageContainer;
import org.sokybot.machinegroup.navigationtree.INavTree;
import org.sokybot.machinegroup.navigationtree.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.icons.FlatSearchIcon;

@Component
public class MachinePageViewer implements IMachinePageViewer {

	@Autowired
	private INavTree navTree;

	@Autowired
	private PageContainer pageContainer;

	@Autowired
	private DashboardContainer dashboardContainer;

	@Value("${" + AppConstants.GROUP_NAME + "}")
	private String groupName;

	@Value("${" + AppConstants.MACHINE_NAME + "}")
	private String machineName;

	@Override
	public void registerPage(String parentNode, String name, Icon icon, JComponent page) {

		String path = groupName + "." + machineName + "." + parentNode + "." + name;
		navTree.putNode(path, TreeNode.makeTreeNode(name, new FlatSearchIcon()));
		this.pageContainer.addPage(path, page);

	}

	@Override
	public void registerPage(String name, Icon icon, JComponent comp) {

		String path = groupName + "." + machineName + "." + name;

		navTree.putLeafNode(groupName + "." + machineName, TreeNode.makeTreeNode(name, icon));
		this.pageContainer.add(path, comp);
	}

	
	//TODO remove page from this.pageContainer

	@Override
	public void removePage(String pageName) {

		this.navTree.removeNode(pageName); 
		this.pageContainer.removePage(pageName);
	//	this.navTree.removeNode(pageName);
		
	}

	@Override
	public void registerDashboard(String name, JComponent content) {
		// currently ignore tab name parameter
		this.dashboardContainer.addPage(groupName + "." + machineName, content);

	}
	
	@Override
	public void removeDashboard(String name) {
	 
		this.dashboardContainer.removeDashboard(name);
	}
	

}
