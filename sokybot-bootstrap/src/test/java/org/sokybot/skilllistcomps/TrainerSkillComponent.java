package org.sokybot.skilllistcomps;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InvalidClassException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;

import org.sing_group.gc4s.input.list.ExtendedDefaultListModel;
import org.sing_group.gc4s.input.list.JListPanel;
import org.sing_group.gc4s.ui.ColorListCellRenderer;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.icons.FlatSearchIcon;

public class TrainerSkillComponent extends JPanel implements ActionListener {


	@Override
	public void actionPerformed(ActionEvent e) {

		Object source = e.getSource();

		if (source == this.btnMoveAttackSkill) {

			int selectedItem = this.lstSkill.getSelectedIndex();

			String eleValue = this.lstSkillModel.getElementAt(selectedItem);
			this.lstAttackSkill.addElement(eleValue);
		} else if (source == this.btnMoveBAttackSkill) {

		}

	}

	public static void main(String args[]) {


	}

}
