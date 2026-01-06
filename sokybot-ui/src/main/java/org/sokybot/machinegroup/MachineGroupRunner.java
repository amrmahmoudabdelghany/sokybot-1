package org.sokybot.machinegroup;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.machinegroup.navigationtree.INavTree;
import org.sokybot.service.IMainFrameConfigurator;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter;

@Component(immediate = true)
public class MachineGroupRunner {

    // Dependencies
    private INavTree navTree;
    private PageContainer pageContainer;
    private DashboardContainer dashboardContainer;
    private IMainFrameConfigurator mainFrameConfigurator;

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

    @Reference
    public void setMainFrameConfigurator(IMainFrameConfigurator mainFrameConfigurator) {
        this.mainFrameConfigurator = mainFrameConfigurator;
    }

    // Config
    private String groupName = "SokyBot"; // Default or from ConfigAdmin

    @Activate
    public void start() {
        System.out.println("MachineGroupRunner: Initializing Machine Group UI...");
        
        Icon gameIcon = loadIcon("icons/feed.svg"); // Replicating 'feed' icon
        
        // Ensure group name is set (if using ConfigAdmin, inject it here)
        
        mainFrameConfigurator.addPage(groupName, gameIcon, groupName, createDefaultMainContainer());
    }

    private JComponent createDefaultMainContainer() {
        JPanel mainCont = new JPanel(new BorderLayout());

        dashboardContainer.setPreferredSize(new Dimension(180, 0));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navTree, pageContainer);
        split.setDividerLocation(250);
        split.setDividerSize(2);
        
        mainCont.add(split, BorderLayout.CENTER);
        mainCont.add(dashboardContainer, BorderLayout.EAST);

        return mainCont;
    }
    
    private Icon loadIcon(String path) {
         try {
            FlatSVGIcon icon = new FlatSVGIcon(path, getClass().getClassLoader());
            icon = icon.derive(0.40f);
            ColorFilter filter = ColorFilter.getInstance();
            filter.add(Color.black, Color.DARK_GRAY, Color.LIGHT_GRAY);
            icon.setColorFilter(filter);
            return icon;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
