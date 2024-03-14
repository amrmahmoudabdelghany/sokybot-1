package org.sokybot.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;


public class ANSITextPane extends JTextPane  implements Appendable {

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

	
	private JPopupMenu jPopupMenu ; 
	
	public ANSITextPane() {
	
		this.jPopupMenu = new JPopupMenu() ; 
		JMenuItem clear = new JMenuItem("Clear") ; 
		clear.addActionListener((ev)->this.setText(""));
		this.jPopupMenu.add(clear) ; 
		this.setComponentPopupMenu(jPopupMenu);
		
	}
	
//	public void append(Color color , String str) { 
//		StyleContext sc = StyleContext.getDefaultStyleContext();
//	    AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
//	    int len = getDocument().getLength(); // same value as getText().length();
//	    setCaretPosition(len);  // place caret at the end (with no selection)
//	    setCharacterAttributes(aset, false);
//	    replaceSelection(str); // there is no selection, so inserts at caret
//	}
//	
	
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
		int aPos = 0; // current char position in addString
		int aIndex = 0; // index of next Escape sequence
		int mIndex = 0; // index of "m" terminating Escape sequence
		String tmpString = "";
		boolean stillSearching = true; // true until no more Escape sequences
		String addString = remaining + str;
		remaining = "";

		if (addString.length() > 0) {
			aIndex = addString.indexOf("\u001B"); // find first escape
			if (aIndex == -1) { // no escape/color change in this string, so just send it with current color
				append(colorCurrent, addString);
				return;
			}
			// otherwise There is an escape character in the string, so we must process it

			if (aIndex > 0) { // Escape is not first char, so send text up to first escape
				tmpString = addString.substring(0, aIndex);
				append(colorCurrent, tmpString);
				aPos = aIndex;
			}
			// aPos is now at the beginning of the first escape sequence

			stillSearching = true;
			while (stillSearching) {
				mIndex = addString.indexOf("m", aPos); // find the end of the escape sequence
				if (mIndex < 0) { // the buffer ends halfway through the ansi string!
					remaining = addString.substring(aPos, addString.length());
					stillSearching = false;
					continue;
				} else {
					tmpString = addString.substring(aPos, mIndex + 1);
					colorCurrent = getANSIColor(tmpString);
				}
				aPos = mIndex + 1;
				// now we have the color, send text that is in that color (up to next escape)

				aIndex = addString.indexOf("\u001B", aPos);

				if (aIndex == -1) { // if that was the last sequence of the input, send remaining text
					tmpString = addString.substring(aPos, addString.length());
					append(colorCurrent, tmpString);
					stillSearching = false;
					continue; // jump out of loop early, as the whole string has been sent now
				}

				// there is another escape sequence, so send part of the string and prepare for
				// the next
				tmpString = addString.substring(aPos, aIndex);
				aPos = aIndex;
				append(colorCurrent, tmpString);

			} // while there's text in the input buffer
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

	public static void main(String args[]) throws IOException {

		JFrame frame = new JFrame();
		JPanel mainCont = (JPanel) frame.getContentPane();
		mainCont.setLayout(new BorderLayout());

		ANSITextPane ansi = new ANSITextPane();
		// ansi.setEditable(false);
		mainCont.add(ansi, BorderLayout.CENTER);

		String banner = Files.lines(Paths.get("src", "main", "resources", "banner.txt"))
				.collect(Collectors.joining("\n"));
		System.out.println(banner);
		ansi.setFont(new Font("Consolas", Font.PLAIN, 15));
		ansi.setEditable(true);
		ansi.setBackground(Color.BLACK);
		ansi.appendAnsi(banner);
		ansi.appendAnsi("\u001B[1;36m asdsad \n");
		ansi.appendAnsi("\u001B[1;31m Hello World , \u001B[1;30m hello amr");

		frame.setLocationRelativeTo(null);
		frame.setPreferredSize(new Dimension(400, 400));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	}
}
