package org.sokybot.machineui.dashboard;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.osgi.framework.BundleContext;
import org.sokybot.runtime.IMachineContext;

public class MachineDashboard extends JPanel {

    private static final long serialVersionUID = 1L;

    private final MachineControll controll;
    private final TrainerInfoPanel charInfoPanel;

    public MachineDashboard(IMachineContext context, BundleContext bundleContext) {
        this.controll = new MachineControll(context, bundleContext);
        this.charInfoPanel = new TrainerInfoPanel(context, bundleContext);
        
        init();
    }

    private void init() { 
        this.controll.setBorder(BorderFactory.createEtchedBorder());
        setLayout(new BorderLayout());
        JPanel charInfo = new JPanel(new BorderLayout()); 
        charInfo.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
        charInfo.add(this.charInfoPanel, BorderLayout.CENTER); 
        add(charInfo, BorderLayout.NORTH);
        add(this.controll, BorderLayout.PAGE_END); 
    }
    
    public void dispose() {
        if (controll != null) controll.dispose();
        if (charInfoPanel != null) charInfoPanel.dispose();
    }
}
