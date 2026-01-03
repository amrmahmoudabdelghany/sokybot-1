package org.sokybot.plugin;

import java.util.Collections;
import java.util.List;

public interface IGameUIExtension {

    /**
     * key used when register this service to osgi logic 
     */
    public final static String UI_EXTENSION = "UI_EXTENSION" ; 
    
    /**
     * Returns a list of pages to be added to the Side Navigation Bar.
     * @return List of PageDefinition, or empty list if none.
     */
    default List<PageDefinition> getPages() {
        return Collections.emptyList();
    }

    // Future: List<DashboardDefinition> getDashboards();
}
