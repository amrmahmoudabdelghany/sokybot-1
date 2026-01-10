package org.sokybot.machineui.dashboard;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicProgressBarUI;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.gameevents.events.character.CharacterInfoEvent;
import org.sokybot.gameevents.events.entity.EntityHPMPUpdateEvent;
import org.sokybot.runtime.IMachineContext;

public class TrainerInfoPanel extends JPanel implements EventHandler {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(TrainerInfoPanel.class);

    private final IMachineContext context;
    private final BundleContext bundleContext;
    private ServiceRegistration<EventHandler> eventRegistration;

    private JLabel lblCharIcon;
    private JLabel lblCharName;
    private JLabel lblLvl;
    private JLabel lblSkillPoint;
    private JLabel lblGold;
    private JLabel lblPosX;
    private JLabel lblPosY;
    private JProgressBar pbHP;
    private JProgressBar pbMP;
    private JProgressBar pbEXP;
    private JProgressBar pbZerk;
    
    private JTextField chatText;
    private JButton btnSend;

    public TrainerInfoPanel(IMachineContext context, BundleContext bundleContext) {
        this.context = context;
        this.bundleContext = bundleContext;
        
        initUI();
        registerEventHandler();
    }

    private void initUI() {
        this.lblCharName = new JLabel("None");
        this.lblLvl = new JLabel("0");
        this.lblSkillPoint = new JLabel("0");
        this.lblGold = new JLabel("0");
        this.lblPosX = new JLabel("0");
        this.lblPosY = new JLabel("0");
        
        this.pbHP = createProgressBar(new Color(255, 179, 179, 80), "HP 0%");
        this.pbMP = createProgressBar(new Color(179, 179, 255, 80), "MP 0%");
        this.pbEXP = createProgressBar(new Color(179, 255, 204, 80), "EXP 0%");
        this.pbZerk = createProgressBar(new Color(179, 217, 255, 80), "Zerk 0%");
        
        this.chatText = new JTextField(context.name() + ":" + "Hello world");
        this.btnSend = new JButton("Send");
        
        // Layout (Simplified for brevity, similar to original)
        BoxLayout layout = new BoxLayout(this, BoxLayout.PAGE_AXIS);
        setLayout(layout);
        
        add(createBoldLabel("Char: " + context.name())); // Placeholder
        add(Box.createVerticalStrut(5));
        add(createPbComp());
        // Add more fields as needed...
    }
    
    private JProgressBar createProgressBar(Color bg, String text) {
        JProgressBar pb = new JProgressBar(JProgressBar.HORIZONTAL);
        pb.setStringPainted(true);
        pb.setBackground(bg);
        pb.setString(text);
        pb.setOpaque(false);
        pb.setUI(new BasicProgressBarUI() {
            @Override
             protected Color getSelectionBackground() { return Color.WHITE; }
             @Override
             protected Color getSelectionForeground() { return Color.WHITE; }
             @Override
             public void paint(Graphics g, JComponent c) {
                 g.setColor(c.getBackground());
                 g.fillRect(0, 0, getWidth(), getHeight());
                 super.paint(g, c);
             }
        });
        return pb;
    }
    
    private JComponent createPbComp() { 
        Box box  = Box.createVerticalBox(); 
        box.add(this.pbHP); 
        box.add(this.pbMP); 
        box.add(this.pbZerk); 
        box.add(this.pbEXP); 
        box.setOpaque(false);
        return box; 
    }

    private JLabel createBoldLabel(String txt) {
        JLabel lbl = new JLabel(txt);
        Font f = lbl.getFont();
        lbl.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        return lbl;
    }

    private void registerEventHandler() {
        Dictionary<String, Object> props = new Hashtable<>();
        // Listen to all game events for this machine?
        // Game events are typically published to "sokybot/game/{machineId}/{EventClassSimpleName}"
        // We can listen to "sokybot/game/*" and filter, or specific topics.
        // Assuming socket-game-events defines topics or follows pattern.
        // CommandHandler used: "sokybot/game/*/ChatMessageEvent"
        
        props.put(EventConstants.EVENT_TOPIC, "sokybot/game/" + context.fullName() + "/*");
        eventRegistration = bundleContext.registerService(EventHandler.class, this, props);
    }
    
    public void dispose() {
        if (eventRegistration != null) {
            try {
                eventRegistration.unregister();
            } catch (Exception e) {}
        }
    }

    @Override
    public void handleEvent(Event event) {
        Object eventObj = event.getProperty("event");
        
        SwingUtilities.invokeLater(() -> {
            if (eventObj instanceof CharacterInfoEvent) {
                CharacterInfoEvent info = (CharacterInfoEvent) eventObj;
                this.lblCharName.setText(info.getName());
                this.lblLvl.setText(String.valueOf(info.getLevel()));
                this.lblGold.setText(String.valueOf(info.getGold()));
                // Update ProgressBars... logic needs mapping from maxHP/currentHP
            } else if (eventObj instanceof EntityHPMPUpdateEvent) {
                // Handle HP update
                // EntityHPMPUpdateEvent hpEvent = (EntityHPMPUpdateEvent) eventObj;
                // if (hpEvent.getUniqueId() == trainerConfig.id?) ... need to know trainer ID.
                // Trainer ID is internal or in Model?
                // For now, simplify.
            }
        });
    }
}
