package org.sokybot.machine.service;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.IMachinePageViewer;
import org.sokybot.app.AppConstants;
import org.sokybot.machinegroup.DashboardContainer;
import org.sokybot.machinegroup.PageContainer;
import org.sokybot.machinegroup.navigationtree.INavTree;
import org.sokybot.machinegroup.navigationtree.TreeNode;

import com.formdev.flatlaf.icons.FlatSearchIcon;

@Component(service = IMachinePageViewer.class)
public class MachinePageViewer implements IMachinePageViewer {

    private INavTree navTree;
    private PageContainer pageContainer;
    private DashboardContainer dashboardContainer;

    @Reference
    public void setNavTree(INavTree navTree) {
        this.navTree = navTree;
    }

    @Reference
    public void setPageContainer(PageContainer pageContainer) {
        this.pageContainer = pageContainer;
    }

    @Reference
    public void setDashboardContainer(DashboardContainer dashboardContainer) {
        this.dashboardContainer = dashboardContainer;
    }

    // Configurable
    private String groupName = "SokyBot"; 
    private String machineName = "Bot1"; 

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
