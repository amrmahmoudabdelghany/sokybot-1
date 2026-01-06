package org.sokybot.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Line2D;

import javax.swing.border.AbstractBorder;

/**
 * Custom RoundedBorder for the Sokybot UI.
 */
public class RoundedBorder extends AbstractBorder {

    private final Color color;
    private final int gap;

    public RoundedBorder(Color color, int g) {
        this.color = color;
        this.gap = g;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        super.paintBorder(c, g, x, y, width, height);
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(color);
            
            g2d.draw(new Line2D.Double((double) x, (double) y + 10, (double) x + 3, (double) y + 3));
            g2d.draw(new Line2D.Double((double) x + 3, (double) y + 3, (double) x + 10, (double) y));
            g2d.draw(new Line2D.Double((double) x + 10, (double) y, (double) x + 30, (double) y));
            g2d.draw(new Line2D.Double((double) x + 30, (double) y, (double) x + 33, (double) y + 2));
            g2d.draw(new Line2D.Double((double) x + 33, (double) y + 2, (double) x + 36, (double) y + 8));
            g2d.draw(new Line2D.Double((double) x + 36, (double) y + 8, (double) x + 36, (double) y + 28));
            g2d.draw(new Line2D.Double((double) x + 36, (double) y + 28, (double) x + 34, (double) y + 31));
            g2d.draw(new Line2D.Double((double) x + 34, (double) y + 31, (double) x + 32, (double) y + 33));
            g2d.draw(new Line2D.Double((double) x + 32, (double) y + 33, (double) x + 6, (double) y + 33));
            g2d.draw(new Line2D.Double((double) x + 6, (double) y + 33, (double) x + 3, (double) y + 31));
            g2d.draw(new Line2D.Double((double) x + 3, (double) y + 31, (double) x, (double) y + 27));
            g2d.draw(new Line2D.Double((double) x, (double) y + 27, (double) x, (double) y + 10));
        }
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return (getBorderInsets(c, new Insets(gap, gap, gap, gap)));
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.top = insets.right = insets.bottom = gap;
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return true;
    }
}
