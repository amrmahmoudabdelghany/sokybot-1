package org.sokybot.machinegroup.service;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.ui.api.IPageViewer;
import org.sokybot.app.AppConstants;
import org.sokybot.machinegroup.PageContainer;
import org.sokybot.machinegroup.navigationtree.INavTree;
import org.sokybot.machinegroup.navigationtree.TreeNode;

import com.formdev.flatlaf.icons.FlatSearchIcon;

@Component(service = IPageViewer.class)
public class MachineGroupPageViewer implements IPageViewer {

    private INavTree navTree;
    private PageContainer pageContainer;

    @Reference
    public void setNavTree(INavTree navTree) {
        this.navTree = navTree;
    }

    @Reference
    public void setPageContainer(PageContainer pageContainer) {
        this.pageContainer = pageContainer;
    }

    // Configurable group name
    private String groupName = "SokyBot"; 

    @Activate
    private void start() { 
        System.out.println("MachineGroupPageViewer :: Activated. GroupName: " + this.groupName);
    }

    @Override
    public void registerPage(String name, Icon icon, JComponent content) {
        String path = this.groupName + "." + name; 
        System.out.println("MachineGroupPageViewer :: Registering Page: " + path); 
        
        navTree.putNode(this.groupName, TreeNode.makeTreeNode(name, new FlatSearchIcon()));
        this.pageContainer.addPage(path, content);
    }
    
    @Override
    public void registerPage(String parent, String name, Icon icon, JComponent content) {
        registerPage(parent + "." + name, icon, content);
    }
    
    @Override
    public void removePage(String pageName) {
        // TODO implement this method
    }
}
