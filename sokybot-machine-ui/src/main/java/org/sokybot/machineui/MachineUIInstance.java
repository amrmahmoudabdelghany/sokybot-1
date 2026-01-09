package org.sokybot.machineui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.sokybot.runtime.IMachineContext;
import org.sokybot.machinegroup.DashboardContainer;
import org.sokybot.machinegroup.PageContainer;
import org.sokybot.machinegroup.navigationtree.INavTree;
import org.sokybot.machinegroup.navigationtree.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.icons.FlatSearchIcon;

/**
 * Manages all UI components for a single machine instance.
 * 
 * This class is responsible for:
 * - Creating machine-specific pages (Training, Environment, Logs, Hunting, etc.)
 * - Creating machine dashboard
 * - Registering pages with the PageContainer
 * - Registering dashboard with the DashboardContainer
 * - Managing navigation tree entries
 */
public class MachineUIInstance {
    
    private static final Logger log = LoggerFactory.getLogger(MachineUIInstance.class);
    
    private final String groupName;
    private final String machineName;
    private final String fullName;
    private final IMachineContext context;
    
    private final PageContainer pageContainer;
    private final DashboardContainer dashboardContainer;
    private final INavTree navTree;
    
    private final List<PageRegistration> registeredPages = new ArrayList<>();
    private String dashboardPath;
    
    public MachineUIInstance(
            String groupName,
            String machineName,
            IMachineContext context,
            PageContainer pageContainer,
            DashboardContainer dashboardContainer,
            INavTree navTree) {
        this.groupName = groupName;
        this.machineName = machineName;
        this.fullName = context.fullName();
        this.context = context;
        this.pageContainer = pageContainer;
        this.dashboardContainer = dashboardContainer;
        this.navTree = navTree;
    }
    
    /**
     * Creates and registers all pages and dashboard for this machine.
     */
    public void createPages() {
        log.debug("Creating pages for machine: {}", fullName);
        
        // Create navigation tree node for this machine
        String machineNodePath = groupName + "." + machineName;
        TreeNode machineNode = TreeNode.makeTreeNode(machineName, new FlatSearchIcon());
        navTree.putNode(machineNodePath, machineNode);
        
        // TODO: Create pages using factory services
        // For now, this is a placeholder structure
        
        // Example: Training Page
        // IMachinePage trainingPage = pageFactory.createTrainingPage(context);
        // registerPage("Training", trainingPage.getIcon(), trainingPage.getComponent());
        
        // Example: Environment Page
        // IMachinePage envPage = pageFactory.createEnvironmentPage(context);
        // registerPage("Environment", envPage.getIcon(), envPage.getComponent());
        
        // Create dashboard
        // JComponent dashboard = dashboardFactory.createDashboard(context);
        // registerDashboard(dashboard);
        
        log.info("Created {} pages and dashboard for machine: {}", registeredPages.size(), fullName);
    }
    
    /**
     * Destroys all pages and dashboard for this machine.
     */
    public void destroyPages() {
        log.debug("Destroying pages for machine: {}", fullName);
        
        // Remove all registered pages
        for (PageRegistration registration : registeredPages) {
            try {
                pageContainer.removePage(registration.path);
                log.debug("Removed page: {}", registration.path);
            } catch (Exception e) {
                log.warn("Error removing page: {}", registration.path, e);
            }
        }
        registeredPages.clear();
        
        // Remove dashboard
        if (dashboardPath != null) {
            try {
                dashboardContainer.removeDashboard(dashboardPath);
                log.debug("Removed dashboard: {}", dashboardPath);
                dashboardPath = null;
            } catch (Exception e) {
                log.warn("Error removing dashboard: {}", dashboardPath, e);
            }
        }
        
        // Remove navigation tree node
        String machineNodePath = groupName + "." + machineName;
        navTree.removeNode(machineNodePath);
        
        log.info("Destroyed all UI components for machine: {}", fullName);
    }
    
    /**
     * Registers a page with the page container and navigation tree.
     */
    private void registerPage(String pageName, Icon icon, JComponent content) {
        String pagePath = fullName + "." + pageName;
        
        pageContainer.addPage(pagePath, content);
        
        String machineNodePath = groupName + "." + machineName;
        TreeNode pageNode = TreeNode.makeTreeNode(pageName, icon != null ? icon : new FlatSearchIcon());
        navTree.putNode(machineNodePath + "." + pageName, pageNode);
        
        registeredPages.add(new PageRegistration(pagePath, pageName));
        
        log.debug("Registered page: {} for machine: {}", pageName, fullName);
    }
    
    /**
     * Registers a dashboard with the dashboard container.
     */
    private void registerDashboard(JComponent dashboard) {
        dashboardPath = fullName + ".dashboard";
        dashboardContainer.addDashboard(dashboardPath, dashboard);
        log.debug("Registered dashboard for machine: {}", fullName);
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public IMachineContext getContext() {
        return context;
    }
    
    /**
     * Internal class to track page registrations.
     */
    private static class PageRegistration {
        final String path;
        final String name;
        
        PageRegistration(String path, String name) {
            this.path = path;
            this.name = name;
        }
    }
}
