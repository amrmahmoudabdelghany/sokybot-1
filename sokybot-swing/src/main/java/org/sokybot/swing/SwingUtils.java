package org.sokybot.swing;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Utility class for common Swing operations.
 */
public final class SwingUtils {

    private SwingUtils() {
    }

    public static Box createHorizontalBox(int strut, Component... components) {
        Box res = Box.createHorizontalBox();
        for (Component com : components) {
            res.add(com);
            res.add(Box.createHorizontalStrut(strut));
        }
        return res;
    }

    public static Box createHorizontalBox(Component... components) {
        return createHorizontalBox(5, components);
    }

    public static Box createVerticalBox(int strut, Component... components) {
        Box res = Box.createVerticalBox();
        for (Component com : components) {
            res.add(com);
            res.add(Box.createVerticalStrut(strut));
        }
        return res;
    }

    public static Box createVerticalBox(Component... components) {
        return createVerticalBox(5, components);
    }

    /**
     * Utility to scale and round an image.
     */
    public static Image processImage(Image image, int w, int h, int cornerRadius) {
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();

        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, w, h, null);

        g2.dispose();
        return output;
    }

    /**
     * Utility to load icons from the classpath.
     */
    public static Icon getIcon(String path) {
        URL url = SwingUtils.class.getClassLoader().getResource(path);
        if (url != null) {
            return new ImageIcon(url);
        }
        return null;
    }
}
