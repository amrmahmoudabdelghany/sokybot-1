package org.sokybot.machineui.page.training;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.settings.Settings;
import org.sokybot.settings.TrainingAreaSettings;
import java.awt.BorderLayout;

public class AreaTab extends JPanel {
    
    private static final long serialVersionUID = 1L;
    private final IMachineContext context;
    private JList<String> areaList;
    private AreaListModel listModel;
    
    public AreaTab(IMachineContext context) {
        this.context = context;
        init();
    }
    
    private void init() {
         setLayout(new BorderLayout());
         
         Settings settings = context.getSettings();
         if (settings != null) {
             TrainingAreaSettings areaSettings = settings.getTrainingAreaSettings();
             this.listModel = new AreaListModel(areaSettings);
             this.areaList = new JList<>(listModel);
             add(new JScrollPane(areaList), BorderLayout.CENTER);
         } else {
             add(new javax.swing.JLabel("Settings not available"), BorderLayout.CENTER);
         }
    }
    
    private static class AreaListModel extends DefaultListModel<String> {
        private static final long serialVersionUID = 1L;
        private final TrainingAreaSettings settings;
        private int activeIndex = -1;

        public AreaListModel(TrainingAreaSettings settings) {
             this.settings = settings;
             init();
        }

        private void init() {
            String[] names = settings.getTrainingAreaNames();
            String active = settings.getActiveArea().getName();

            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(active)) {
                    add(i, names[i] + " [Active]");
                    activeIndex = i;
                } else {
                    add(i, names[i]);
                }
            }
        }
        
        // Simplified read-only for now, add logic similar to original AreaListModel later
    }
}
