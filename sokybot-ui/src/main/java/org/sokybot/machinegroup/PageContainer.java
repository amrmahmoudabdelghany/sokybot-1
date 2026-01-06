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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PageContainer.class, property = EventConstants.EVENT_TOPIC + "=org/sokybot/ui/NAV_TREE_SELECTION")
public class PageContainer extends JPanel implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(PageContainer.class);
    private CardLayout cardLayout;

    private final Map<String, java.awt.Component> comps = new HashMap<>();

    @Activate
    void init() {
        this.cardLayout = new CardLayout();
        setLayout(this.cardLayout);
        setBorder(BorderFactory.createEtchedBorder());
    }

    public void addPage(String name, java.awt.Component component) {
        this.add(name, component);
        this.comps.put(name, component);
    }

    public void removePage(String name) {
        if (comps.containsKey(name)) {
            java.awt.Component comp = this.comps.get(name);
            this.remove(comp);
            this.comps.remove(name);
        }
    }

    @Override
    public void handleEvent(Event event) {
        Object eventObj = event.getProperty("event");
        if (eventObj instanceof NavTreeSelectionEvent) {
             NavTreeSelectionEvent navEvent = (NavTreeSelectionEvent) eventObj;
             showPage(navEvent);
        }
    }

    public void showPage(NavTreeSelectionEvent navTreeSelectionEvent) {
        String path = navTreeSelectionEvent.getSelectedPath();
        cardLayout.show(this, path);
        log.info("Main Page Container Receive Path {}", path);
    }
}
