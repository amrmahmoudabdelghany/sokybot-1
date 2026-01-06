package org.sokybot.swing;

import java.awt.Color;

/**
 * Interface for components that can receive colored and ANSI-formatted text.
 */
public interface Appendable {
    void append(Color color, String str);
    void appendAnsi(String str);
}
