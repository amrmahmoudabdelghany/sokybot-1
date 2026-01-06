package org.sokybot.swing;

import java.awt.Color;
import javax.swing.Icon;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter;

/**
 * Utility class for loading and styling SVG icons.
 */
public class SokyBotIcons {

    public static final Icon DELETE_ICON = getIcon("icons/delete.svg", 45, 45);
    
    public static Icon getIcon(String name, int w, int h) {
        try {
            FlatSVGIcon icon = new FlatSVGIcon(name, w, h, SokyBotIcons.class.getClassLoader());
            icon = icon.derive(0.40f);
            ColorFilter filter = ColorFilter.getInstance();
            filter.add(Color.black, Color.DARK_GRAY, Color.LIGHT_GRAY);
            icon.setColorFilter(filter);
            return icon;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
