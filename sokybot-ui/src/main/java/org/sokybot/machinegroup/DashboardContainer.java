package org.sokybot.machinegroup;

import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sokybot.machinegroup.navigationtree.NavTreeSelectionEvent;

@Component(service = DashboardContainer.class, property = EventConstants.EVENT_TOPIC + "=org/sokybot/ui/NAV_TREE_SELECTION")
public class DashboardContainer extends JPanel implements EventHandler {

    private static final long serialVersionUID = 1L;
    private CardLayout cardLayout;

    private final Map<String, java.awt.Component> comps = new HashMap<>();

    @Activate
    void init() {
        this.cardLayout = new CardLayout();
        this.setLayout(this.cardLayout);
        setBorder(BorderFactory.createEtchedBorder());
    }

    public void addDashboard(String name, java.awt.Component component) {
        this.add(name, component);
        this.comps.put(name, component);
    }

    public void addPage(String name, java.awt.Component component) {
        this.add(name, component);
        this.comps.put(name, component);
    }

    public void removeDashboard(String name) {
        if (this.comps.containsKey(name)) {
            java.awt.Component comp = this.comps.get(name);
            this.remove(comp);
            this.comps.remove(name);
        }
    }

    @Override
    public void handleEvent(Event event) {
        Object eventObj = event.getProperty("event");
        if (eventObj instanceof NavTreeSelectionEvent) {
            showDashboard((NavTreeSelectionEvent) eventObj);
        }
    }

    public void showDashboard(NavTreeSelectionEvent navTreeSelectionEvent) {
        String path = navTreeSelectionEvent.getSelectedPath();
        String pathComps[] = path.split("\\.");
        if (pathComps.length >= 2) {
            String targetDashboard = pathComps[0] + "." + pathComps[1];
            cardLayout.show(this, targetDashboard);
        }
    }
}
