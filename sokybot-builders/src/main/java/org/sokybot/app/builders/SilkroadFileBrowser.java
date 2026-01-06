package org.sokybot.app.builders;

import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.sokybot.utils.SilkroadUtils;

public class SilkroadFileBrowser extends JFileChooser {
    private JTextField fileNameTextField;
    private JButton approveButton;

    public SilkroadFileBrowser() {
        setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        setSelectedFile(new File("C:\\Silkroad")); // Updated default path for Windows
        addChoosableFileFilter(new FileNameExtensionFilter("Silkroad Directory", "*"));
        setAcceptAllFileFilterUsed(true);
        setDialogTitle("Select Silkroad Directory");
        setApproveButtonText("Select");
        fileNameTextField = getFileNameTextField(getComponents());
        approveButton = getSelectButton(this);
        if (approveButton != null) approveButton.setEnabled(false);
        if (fileNameTextField != null) fileNameTextField.setEditable(false);
        
        addPropertyChangeListener((PropertyChangeEvent evt) -> {
            if (evt.getPropertyName().equals("directoryChanged")) {
                onDirectoryChanged(getCurrentDirectory());
            }
        });
        
        if (SilkroadUtils.isValidSilkroadDirectory(getCurrentDirectory().getAbsolutePath())) {
            if (approveButton != null) approveButton.setEnabled(true);
            if (fileNameTextField != null) fileNameTextField.setText(getCurrentDirectory().getAbsolutePath());
        }
    }

    private JTextField getFileNameTextField(Component[] comp) {
        JTextField temp = null;
        if (comp != null) {
            for (int i = 0; i < comp.length; i++) {
                if (comp[i] instanceof JPanel) {
                    if ((temp = getFileNameTextField(((JPanel) comp[i]).getComponents())) != null) {
                        return temp;
                    }
                } else if (comp[i] instanceof JTextField) {
                    temp = ((JTextField) comp[i]);
                }
            }
        }
        return temp;
    }

    private void onDirectoryChanged(File currentDirectory) {
        if (SilkroadUtils.isValidSilkroadDirectory(currentDirectory.getAbsolutePath())) {
            if (approveButton != null) approveButton.setEnabled(true);
        } else {
            if (approveButton != null) approveButton.setEnabled(false);
        }
    }

    private JButton getSelectButton(Container c) {
        JButton temp = null;
        for (Component comp : c.getComponents()) {
            if (comp == null) continue;
            if (comp instanceof JButton && (temp = (JButton) comp).getText() != null
                    && temp.getText().equals("Select")) {
                return temp;
            } else if (comp instanceof Container) {
                if ((temp = getSelectButton((Container) comp)) != null) {
                    return temp;
                }
            }
        }
        return temp;
    }
}
