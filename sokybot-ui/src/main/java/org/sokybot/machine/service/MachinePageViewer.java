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
	public void registerPage(String name, Icon icon, JComponent content) {
		String machineNode = this.groupName + "." + this.machineName;
		String path = machineNode + "." + name;
		navTree.putNode(machineNode, TreeNode.makeTreeNode(name, icon == null ? new FlatSearchIcon() : icon));
		this.pageContainer.addPage(path, content);
	}

	@Override
	public void registerPage(String parent, String name, Icon icon, JComponent content) {
		registerPage(parent + "." + name, icon, content);
	}

	@Override
	public void removePage(String pageName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerDashboard(String name, JComponent content) {
		this.dashboardContainer.addDashboard(name, content);

	}

	@Override
	public void removeDashboard(String name) {
		// TODO Auto-generated method stub

	}

}
