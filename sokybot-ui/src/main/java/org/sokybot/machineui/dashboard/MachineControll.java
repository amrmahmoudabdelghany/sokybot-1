package org.sokybot.machineui.dashboard;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.machine.MachineState;
import org.sokybot.runtime.IMachineContext;

public class MachineControll extends JPanel implements ActionListener, EventHandler {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MachineControll.class);

    private final IMachineContext context;
    private final BundleContext bundleContext;
    private ServiceRegistration<EventHandler> eventRegistration;

    private JButton btnConn;
    private JButton btnCommit;
    private JButton btnLogStates;
    private JButton btnKillClient;
    private JButton btnTraining;

    public MachineControll(IMachineContext context, BundleContext bundleContext) {
        this.context = context;
        this.bundleContext = bundleContext;
        
        initUI();
        registerEventHandler();
    }

    private void initUI() {
        setLayout(new GridBagLayout());

        this.btnConn = new JButton("Connect");
        this.btnCommit = new JButton("Apply");
        this.btnKillClient = new JButton("Kill Client");
        this.btnLogStates = new JButton("Log States");
        this.btnTraining = new JButton("Start Training");

        this.btnConn.setActionCommand("CONNECT");
        this.btnConn.addActionListener(this);
        
        this.btnCommit.setActionCommand("CONFIG_COMMIT");
        this.btnCommit.addActionListener(this);
        this.btnCommit.setEnabled(false);
        
        this.btnKillClient.setActionCommand("KILL_CLIENT");
        this.btnKillClient.addActionListener(this);
        
        this.btnTraining.setActionCommand("START_TRAINING");
        this.btnTraining.addActionListener(this);

        this.btnLogStates.addActionListener(e -> {
            // Log states logic - engine doesn't expose current state directly except via isRunning
            // We might just log a message 
            log.info("Log States requested for {}", context.name());
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);

        this.add(this.btnConn, gbc);
        gbc.gridy = 1;
        this.add(this.btnKillClient, gbc);
        gbc.gridy = 2;
        this.add(this.btnCommit, gbc);

        gbc.gridy = 3;
        this.add(this.btnTraining, gbc);

        gbc.gridy = 4;
        this.add(this.btnLogStates, gbc);
        
        // Initialize state based on engine running status
        if (context.isRunning()) {
            onConnect();
        }
    }

    private void registerEventHandler() {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(EventConstants.EVENT_TOPIC, "sokybot/machine/state");
        // props.put("machineId", context.fullName()); // Filter? OSGi filter is better but handler impl is simple check
        
        eventRegistration = bundleContext.registerService(EventHandler.class, this, props);
    }
    
    public void dispose() {
        if (eventRegistration != null) {
            try {
                eventRegistration.unregister();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        String machineId = (String) event.getProperty("machineId");
        if (machineId == null || !machineId.equals(context.fullName())) {
            return;
        }
        
        String stateName = (String) event.getProperty("state");
        if (stateName == null) return;
        
        SwingUtilities.invokeLater(() -> updateState(stateName));
    }
    
    // Mapping state names (MachineState strings) to UI logic
    // We assume stateName matches the enum strings
    private void updateState(String state) {
        try {
            MachineState ms = MachineState.valueOf(state);
            
            switch (ms) {
                case CONFIG_COMMITTED:
                    this.btnCommit.setEnabled(false);
                    break;
                case CONFIG_UNCOMMITTED:
                    this.btnCommit.setEnabled(true);
                    break;
                case READY: // Connected
                    onConnect();
                    break;
                case WITHOUT_CLIENT:
                    this.btnKillClient.setEnabled(false);
                    break;
                case WITH_CLIENT:
                    this.btnKillClient.setEnabled(true);
                    break;
                case PLAYING:
                    onStartTraining();
                    break;
                case IDLE:
                    onStopTraining();
                    break;
                default:
                    // Check for disconnected logic? 
                    // MachineState doesn't have DISCONNECTED, it has STATE machine states.
                    // If we receive "root"? No.
                    break;
            }
        } catch (IllegalArgumentException e) {
            // Ignore unknown states
        }
    }
    
    private void onConnect() {
        this.btnConn.setActionCommand("DISCONNECT");
        this.btnConn.setText("Disconnect");
    }
    
    private void onDisconnect() { // Needs to be called somehow? Maybe Engine sends DISCONNECTED event?
        this.btnConn.setActionCommand("CONNECT");
        this.btnConn.setText("Connect");
    }

    private void onStartTraining() {
        this.btnTraining.setText("Stop Training");
        this.btnTraining.setActionCommand("STOP_TRAINING");
    }

    private void onStopTraining() {
        this.btnTraining.setText("Start Training");
        this.btnTraining.setActionCommand("START_TRAINING");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        
        if ("DISCONNECT".equals(action)) {
            int res = JOptionPane.showConfirmDialog(this.getTopLevelAncestor(),
                    "Are you sure you want to disconnect ? ");
            if (res != JOptionPane.OK_OPTION)
                return;
             
             // Optimistic update
             onDisconnect();
        }

        try {
            context.getEngine().sendEvent(action);
        } catch (Exception ex) {
            log.error("Failed to send command: " + action, ex);
            JOptionPane.showMessageDialog(this, "Failed to send command: " + ex.getMessage());
        }
    }
}
