package org.sokybot.machine.page.trainingpage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.sing_group.gc4s.dialog.AbstractInputJDialog;
import org.sing_group.gc4s.input.filechooser.JFileChooserPanel;
import org.sing_group.gc4s.input.filechooser.Mode;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.machinegroup.gamemodel.setting.TrainingArea;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.extras.components.FlatTextField;

public class Test2 {

	public static boolean isTrue() {
		return false;
	}

	static void showInputDialog() {

		FlatTextField txt = new FlatTextField();

		JLabel lblError = new JLabel("Error");
		JComponent[] comps = new JComponent[] { new JLabel("Area Name "), txt, lblError };

		JDialog dialog = new JDialog();
		JOptionPane optPane = new JOptionPane(comps, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

		optPane.addPropertyChangeListener((prop) -> {
			if (prop.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {

				if(prop.getNewValue() instanceof Integer) { 
					
					int value = (int) prop.getNewValue() ; 
					
					if(value == JOptionPane.OK_OPTION) { 
						String userInput = txt.getText();
						if (userInput == null || userInput.isBlank()) {
							lblError.setText("Please enter a valid name");
							optPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

						} else if (!userInput.equals("Amr")) {
							lblError.setText("Area Name Must Be Unieq");
							optPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
						} else {
							
							System.out.println("User input is : " + userInput) ; 
							dialog.dispose();
						}
						
					}else if(value == JOptionPane.CANCEL_OPTION || value == JOptionPane.CLOSED_OPTION) {
						System.out.println("User Canceling dialog") ; 
						dialog.dispose() ;
					}
					
				}
			

			}
		});

		dialog.setModal(true);
		dialog.setContentPane(optPane);
		dialog.setVisible(true);

		//Object resObj = optPane.getValue();

		//return (resObj != null) ? ((int) resObj) : -1;
	}

	public static void main(String args[]) {

		JButton btn = new JButton("Test");
		btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				showInputDialog();
				System.out.println("test");
			}
		});
		// btn.addActionListener(new ActionListener() {
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//
//				FlatTextField txtAreaName = new FlatTextField();
//				JLabel lblError = new JLabel("Error");
//				lblError.setForeground(Color.RED);
//
//				InputVerifier iv = new InputVerifier() {
//
//					@Override
//					public boolean verify(JComponent input) {
//						System.out.println("Verived");
//						FlatTextField field = (FlatTextField) input;
//
//						field.setOutline("error");
//						lblError.setText("Area Name Must Be Unieq");
//
//						return false;
//					}
//				};
//				
//				txtAreaName.setInputVerifier(iv);
//				final JComponent[] inputs = new JComponent[] { new JLabel("Enter Area Name : "), txtAreaName,
//						new JTextField(), lblError };
//
//				int result = JOptionPane.showConfirmDialog(null, inputs, "My custom dialog",
//						JOptionPane.OK_CANCEL_OPTION);
//				if (result == JOptionPane.OK_OPTION) {
//					System.out.println("You entered " + txtAreaName.getText());
//
//				} else {
//					System.out.println("User canceled / closed the dialog, result = " + result);
//				}
//			}
//		});
		FlatDarkLaf.setup();
		JFrame frame = new JFrame();

		frame.setLayout(new BorderLayout());
		frame.add(btn, BorderLayout.CENTER);
		frame.setLocationRelativeTo(null);
		frame.setPreferredSize(new Dimension(600, 400));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	}
}
