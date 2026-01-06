package org.sokybot.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 * A JTextPane that can render ANSI color codes.
 */
public class ANSITextPane extends JTextPane implements Appendable {

    static final Color D_Black = Color.getHSBColor(0.000f, 0.000f, 0.000f);
    static final Color D_Red = Color.getHSBColor(0.000f, 1.000f, 0.502f);
    static final Color D_Blue = Color.getHSBColor(0.667f, 1.000f, 0.502f);
    static final Color D_Magenta = Color.getHSBColor(0.833f, 1.000f, 0.502f);
    static final Color D_Green = Color.getHSBColor(0.333f, 1.000f, 0.502f);
    static final Color D_Yellow = Color.getHSBColor(0.167f, 1.000f, 0.502f);
    static final Color D_Cyan = Color.getHSBColor(0.500f, 1.000f, 0.502f);
    static final Color D_White = Color.getHSBColor(0.000f, 0.000f, 0.753f);
    static final Color B_Black = Color.getHSBColor(0.000f, 0.000f, 0.502f);
    static final Color B_Red = Color.getHSBColor(0.000f, 1.000f, 1.000f);
    static final Color B_Blue = Color.getHSBColor(0.667f, 1.000f, 1.000f);
    static final Color B_Magenta = Color.getHSBColor(0.833f, 1.000f, 1.000f);
    static final Color B_Green = Color.getHSBColor(0.333f, 1.000f, 1.000f);
    static final Color B_Yellow = Color.getHSBColor(0.167f, 1.000f, 1.000f);
    static final Color B_Cyan = Color.getHSBColor(0.500f, 1.000f, 1.000f);
    static final Color B_White = Color.getHSBColor(0.000f, 0.000f, 1.000f);
    static final Color cReset = Color.getHSBColor(0.000f, 0.000f, 1.000f);
    static Color colorCurrent = cReset;
    private String remaining = "";

    private JPopupMenu jPopupMenu;

    public ANSITextPane() {
        this.jPopupMenu = new JPopupMenu();
        JMenuItem clear = new JMenuItem("Clear");
        clear.addActionListener((ev) -> this.setText(""));
        this.jPopupMenu.add(clear);
        this.setComponentPopupMenu(jPopupMenu);
    }

    @Override
    public void append(Color c, String s) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
        int len = getDocument().getLength();
        try {
            getDocument().insertString(len, s, aset);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void appendAnsi(String str) {
        int aPos = 0;
        int aIndex = 0;
        int mIndex = 0;
        String tmpString = "";
        boolean stillSearching = true;
        String addString = remaining + str;
        remaining = "";

        if (addString.length() > 0) {
            aIndex = addString.indexOf("\u001B");
            if (aIndex == -1) {
                append(colorCurrent, addString);
                return;
            }

            if (aIndex > 0) {
                tmpString = addString.substring(0, aIndex);
                append(colorCurrent, tmpString);
                aPos = aIndex;
            }

            stillSearching = true;
            while (stillSearching) {
                mIndex = addString.indexOf("m", aPos);
                if (mIndex < 0) {
                    remaining = addString.substring(aPos, addString.length());
                    stillSearching = false;
                    continue;
                } else {
                    tmpString = addString.substring(aPos, mIndex + 1);
                    colorCurrent = getANSIColor(tmpString);
                }
                aPos = mIndex + 1;

                aIndex = addString.indexOf("\u001B", aPos);

                if (aIndex == -1) {
                    tmpString = addString.substring(aPos, addString.length());
                    append(colorCurrent, tmpString);
                    stillSearching = false;
                    continue;
                }

                tmpString = addString.substring(aPos, aIndex);
                aPos = aIndex;
                append(colorCurrent, tmpString);
            }
        }
    }

    public Color getANSIColor(String ANSIColor) {
        if (ANSIColor.equals("\u001B[30m")) {
            return D_Black;
        } else if (ANSIColor.equals("\u001B[31m")) {
            return D_Red;
        } else if (ANSIColor.equals("\u001B[32m")) {
            return D_Green;
        } else if (ANSIColor.equals("\u001B[33m")) {
            return D_Yellow;
        } else if (ANSIColor.equals("\u001B[34m")) {
            return D_Blue;
        } else if (ANSIColor.equals("\u001B[35m")) {
            return D_Magenta;
        } else if (ANSIColor.equals("\u001B[36m")) {
            return D_Cyan;
        } else if (ANSIColor.equals("\u001B[37m")) {
            return D_White;
        } else if (ANSIColor.equals("\u001B[0;30m")) {
            return D_Black;
        } else if (ANSIColor.equals("\u001B[0;31m")) {
            return D_Red;
        } else if (ANSIColor.equals("\u001B[0;32m")) {
            return D_Green;
        } else if (ANSIColor.equals("\u001B[0;33m")) {
            return D_Yellow;
        } else if (ANSIColor.equals("\u001B[0;34m")) {
            return D_Blue;
        } else if (ANSIColor.equals("\u001B[0;35m")) {
            return D_Magenta;
        } else if (ANSIColor.equals("\u001B[0;36m")) {
            return D_Cyan;
        } else if (ANSIColor.equals("\u001B[0;37m")) {
            return D_White;
        } else if (ANSIColor.equals("\u001B[1;30m")) {
            return B_Black;
        } else if (ANSIColor.equals("\u001B[1;31m")) {
            return B_Red;
        } else if (ANSIColor.equals("\u001B[1;32m")) {
            return B_Green;
        } else if (ANSIColor.equals("\u001B[1;33m")) {
            return B_Yellow;
        } else if (ANSIColor.equals("\u001B[1;34m")) {
            return B_Blue;
        } else if (ANSIColor.equals("\u001B[1;35m")) {
            return B_Magenta;
        } else if (ANSIColor.equals("\u001B[1;36m")) {
            return B_Cyan;
        } else if (ANSIColor.equals("\u001B[1;37m")) {
            return B_White;
        } else if (ANSIColor.equals("\u001B[0m")) {
            return cReset;
        } else {
            return B_White;
        }
    }
}
