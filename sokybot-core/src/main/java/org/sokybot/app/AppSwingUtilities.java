package org.sokybot.app;

import java.awt.Component;

import javax.swing.Box;
import javax.swing.JComponent;

import org.hibernate.id.CompositeNestedGeneratedValueGenerator;

public class AppSwingUtilities {

	private AppSwingUtilities() {
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
	
	public static Box createVerticalBox( Component... components) {
		return createVerticalBox(5 , components);
	}

}
