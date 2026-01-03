package org.sokybot.plugin;

public class PageDefinition {

    private String title;
    private String iconPath; // Path to resource, e.g., "/icons/map.png"
    private Object component; // The UI Component (e.g., JPanel), cast by the UI Host.

    public PageDefinition(String title, String iconPath, Object component) {
        this.title = title;
        this.iconPath = iconPath;
        this.component = component;
    }

    public String getTitle() {
        return title;
    }

    public String getIconPath() {
        return iconPath;
    }

    public Object getComponent() {
        return component;
    }
}
