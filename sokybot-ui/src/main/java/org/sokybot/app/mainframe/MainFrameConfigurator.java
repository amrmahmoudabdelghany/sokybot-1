package org.sokybot.app.mainframe;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXFrame;
import org.noos.xing.mydoggy.Content;
import org.noos.xing.mydoggy.ContentManager;
import org.noos.xing.mydoggy.ContentManagerUI;
import org.noos.xing.mydoggy.ContentUI;
import org.noos.xing.mydoggy.DockedTypeDescriptor;
import org.noos.xing.mydoggy.FloatingTypeDescriptor;
import org.noos.xing.mydoggy.RepresentativeAnchorDescriptor;
import org.noos.xing.mydoggy.SlidingTypeDescriptor;
import org.noos.xing.mydoggy.TabbedContentManagerUI;
import org.noos.xing.mydoggy.ToolWindow;
import org.noos.xing.mydoggy.ToolWindowActionHandler;
import org.noos.xing.mydoggy.ToolWindowAnchor;
import org.noos.xing.mydoggy.ToolWindowManager;
import org.noos.xing.mydoggy.ToolWindowManagerDescriptor;
import org.noos.xing.mydoggy.ToolWindowType;
import org.noos.xing.mydoggy.plaf.MyDoggyToolWindowManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sokybot.service.IMainFrameConfigurator;

@org.osgi.service.component.annotations.Component(service = IMainFrameConfigurator.class, immediate = true)
public class MainFrameConfigurator implements IMainFrameConfigurator {

    private JXFrame mainFrame;
    private MyDoggyToolWindowManager toolWindowManager;
    private JMenuBar menuBar;
    private JToolBar toolBar;

    private EventAdmin eventAdmin;

    @Reference
    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    @Activate
    public void start() {
        System.out.println("MainFrameConfigurator: Activator started. Initializing UI...");
        
        // 1. Initialize Look and Feel
        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf");
        }

        // 2. Create Main Frame
        mainFrame = new JXFrame("Sokybot", true);
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Main app should exit
        
        // 2.1 Set Icon
        try {
            com.formdev.flatlaf.extras.FlatSVGIcon appIcon = new com.formdev.flatlaf.extras.FlatSVGIcon("icons/server.svg", 32, 32, getClass().getClassLoader());
            mainFrame.setIconImage(appIcon.getImage());
        } catch (Exception e) {
            // Ignore icon failure
        }

        // 3. Initialize ToolWindowManager
        toolWindowManager = new MyDoggyToolWindowManager();
        configureToolWindowManager(toolWindowManager);

        // 4. Set Content Pane
        mainFrame.getContentPane().add(toolWindowManager);

        // 5. Create Menu and Toolbar
        menuBar = new JSMenuBar();
        mainFrame.setJMenuBar(menuBar);
        
        toolBar = new JToolBar();
        // Add toolbar to main frame? MyDoggy might handle it differently or we add it to NORTH
        mainFrame.add(toolBar, java.awt.BorderLayout.NORTH);


        // 5. Show Frame
        SwingUtilities.invokeLater(() -> {
            mainFrame.setVisible(true);
            System.out.println("MainFrameConfigurator: Frame visible. Publishing event...");
            
            // 6. Publish Event
            WindowPreparedEvent displayEvent = new WindowPreparedEvent(this);
            Dictionary<String, Object> props = new Hashtable<>();
            props.put("event", displayEvent);
            
            Event osgiEvent = new Event("org/sokybot/ui/WINDOW_PREPARED", props);
            eventAdmin.postEvent(osgiEvent);
        });
    }

    @Deactivate
    public void stop() {
        if (mainFrame != null) {
            mainFrame.dispose();
        }
    }

    private void configureToolWindowManager(ToolWindowManager toolWindowManager) {
        ToolWindowManagerDescriptor toolWindowManagerDescriptor = toolWindowManager.getToolWindowManagerDescriptor();
        toolWindowManagerDescriptor.setNumberingEnabled(false);
        toolWindowManagerDescriptor.setPreviewEnabled(false);

        ContentManagerUI<?> contentManagerUI = toolWindowManager.getContentManager().getContentManagerUI();
        contentManagerUI.setCloseable(false);
        contentManagerUI.setDetachable(false);
        contentManagerUI.setMinimizable(false);
        contentManagerUI.setMaximizable(false);

        if (contentManagerUI instanceof TabbedContentManagerUI) {
            TabbedContentManagerUI<?> tabbedContentManagerUI = (TabbedContentManagerUI<?>) contentManagerUI;
            tabbedContentManagerUI.setShowAlwaysTab(true);
        }
    }

    @Override
    public void addExtraWindow(String id, String title, Icon icon, Component comp) {
        SwingUtilities.invokeLater(() -> {
             if (toolWindowManager == null) return;
             
             ToolWindow toolWindow = toolWindowManager.registerToolWindow(id, title, icon, comp, ToolWindowAnchor.BOTTOM);
             toolWindow.setAvailable(true);
             
             RepresentativeAnchorDescriptor representativeAnchorDescriptor = toolWindow.getRepresentativeAnchorDescriptor();
             representativeAnchorDescriptor.setPreviewEnabled(true);
             representativeAnchorDescriptor.setPreviewDelay(1500);
             representativeAnchorDescriptor.setPreviewTransparentRatio(0.4f);
     
             DockedTypeDescriptor dockedTypeDescriptor = (DockedTypeDescriptor) toolWindow.getTypeDescriptor(ToolWindowType.DOCKED);
             dockedTypeDescriptor.setAnimating(true);
             dockedTypeDescriptor.setHideRepresentativeButtonOnVisible(false);
             dockedTypeDescriptor.setDockLength(300);
             dockedTypeDescriptor.setPopupMenuEnabled(true);
             dockedTypeDescriptor.setIdVisibleOnTitleBar(false);
             
             // Demo menu logic maintained from original
             JMenu toolsMenu = dockedTypeDescriptor.getToolsMenu();
             toolsMenu.add(new AbstractAction("Hello World!!!") {
                 public void actionPerformed(ActionEvent e) {
                     JOptionPane.showMessageDialog(null, "Hello World!!!");
                 }
             });
             
             dockedTypeDescriptor.setToolWindowActionHandler(new ToolWindowActionHandler() {
                 public void onHideButtonClick(ToolWindow toolWindow) {
                     toolWindow.setType(ToolWindowType.SLIDING);
                 }
             });
             
             // Sliding setup
             SlidingTypeDescriptor slidingTypeDescriptor = (SlidingTypeDescriptor) toolWindow.getTypeDescriptor(ToolWindowType.SLIDING);
             slidingTypeDescriptor.setEnabled(true);
             slidingTypeDescriptor.setTransparentMode(true);
             slidingTypeDescriptor.setTransparentRatio(0.8f);
             slidingTypeDescriptor.setAnimating(true);
        });
    }

    @Override
    public void addPage(String pageId, Icon icon, String title, Component component) {
         SwingUtilities.invokeLater(() -> {
            if (toolWindowManager == null) return;

            ContentManager contentManager = this.toolWindowManager.getContentManager();
            boolean exists = java.util.stream.Stream.of(contentManager.getContents()).anyMatch((cont) -> cont.getId().equals(pageId));

            if (exists) return;

            Content content = contentManager.addContent(pageId, title, icon, component);
            ContentUI contentUI = contentManager.getContentManagerUI().getContentUI(content);
            contentUI.setTransparentMode(false);
            contentUI.setTransparentRatio(0.7f);
            contentUI.setTransparentDelay(1000);
         });
    }

    @Override
    public void addToolbarButton(javax.swing.JButton button) {
        SwingUtilities.invokeLater(() -> {
            if (toolBar != null) {
                toolBar.add(button);
                toolBar.revalidate();
                toolBar.repaint();
            }
        });
    }

    @Override
    public void displayDialog(javax.swing.JDialog dialog) {
        SwingUtilities.invokeLater(() -> {
            dialog.setLocationRelativeTo(mainFrame);
            dialog.setVisible(true);
        });
    }
}
