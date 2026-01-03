package org.sokybot.ui;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.jdesktop.swingx.JXFrame;
import org.noos.xing.mydoggy.*;
import org.noos.xing.mydoggy.plaf.MyDoggyToolWindowManager;
import javax.swing.*;
import java.awt.*;

public class UIActivator implements BundleActivator {

    private JXFrame mainFrame;
    private ToolWindowManager toolWindowManager;

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("Sokybot UI Starting...");
        
        // Create and show main frame on EDT
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("EDT: About to initialize UI...");
                initializeUI();
                System.out.println("EDT: Original UI initialized and shown.");
            } catch (Throwable t) {
                System.err.println("EDT: ERROR during UI initialization:");
                t.printStackTrace();
            }
        });
        System.out.println("UI bundle start() method completed.");
    }

    private void initializeUI() {
        System.out.println("DEBUG: Restoring Original MyDoggy UI...");
        
        // 1. Create Main Frame (JXFrame)
        mainFrame = new JXFrame("Sokybot", true);
        mainFrame.setExtendedState(mainFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // 2. Initialize MyDoggy ToolWindowManager
        toolWindowManager = new MyDoggyToolWindowManager();
        ToolWindowManagerDescriptor descriptor = toolWindowManager.getToolWindowManagerDescriptor();
        descriptor.setNumberingEnabled(false);
        descriptor.setPreviewEnabled(false);
        
        ContentManagerUI<?> contentManagerUI = toolWindowManager.getContentManager().getContentManagerUI();
        contentManagerUI.setCloseable(false);
        contentManagerUI.setDetachable(false);
        contentManagerUI.setMinimizable(false);
        contentManagerUI.setMaximizable(false);
        
        if (contentManagerUI instanceof TabbedContentManagerUI) {
            TabbedContentManagerUI<?> tabbedContentManagerUI = (TabbedContentManagerUI<?>) contentManagerUI;
            tabbedContentManagerUI.setShowAlwaysTab(true);
        }
        
        // 3. Set ToolWindowManager as content pane
        mainFrame.getContentPane().add((Component) toolWindowManager);
        
        // 4. Add Window Listener for OSGi shutdown (optional but good)
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
               System.out.println("DEBUG: Main Window CLOSING");
            }
        });

        System.out.println("DEBUG: Setting MainFrame visible...");
        mainFrame.pack();
        mainFrame.setVisible(true);
        mainFrame.toFront();
        System.out.println("DEBUG: Full UI initialized and visible.");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("Sokybot UI Stopping...");
        if (mainFrame != null) {
            SwingUtilities.invokeLater(() -> mainFrame.dispose());
        }
    }
}
