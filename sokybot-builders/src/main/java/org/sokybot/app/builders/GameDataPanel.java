package org.sokybot.app.builders;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GameDataPanel extends JPanel implements ItemListener {
    private JComboBox<String> cmbHosts;
    private JComboBox<String> cmbDivs;
    private JLabel lblPort;
    private JLabel lblVer;
    private JLabel lblCountry;
    private JLabel lblLang;
    private JLabel lblLocal;
    private Map<String, List<String>> divHosts;

    public GameDataPanel() {
        this.cmbHosts = new JComboBox<>();
        this.cmbDivs = new JComboBox<>();
        this.lblPort = new JLabel("5000");
        this.lblVer = new JLabel("320");
        this.lblCountry = new JLabel("Egypt");
        this.lblLang = new JLabel("Arabic");
        this.lblLocal = new JLabel("22");
        init();
    }

    public void init() {
        int pW = 150;
        int pH = 20;
        this.cmbDivs.setPreferredSize(new Dimension(pW, pH));
        this.cmbHosts.setPreferredSize(new Dimension(pW, pH));
        this.cmbDivs.addItemListener(this);
        this.setLayout(new GridBagLayout());

        GridBagConstraints GBC = new GridBagConstraints();
        GBC.anchor = GridBagConstraints.WEST;

        Box hBox = Box.createHorizontalBox();
        JLabel aLbl = new JLabel("Division(s) :");
        aLbl.setPreferredSize(new Dimension(pW, pH));
        hBox.add(aLbl);
        aLbl = new JLabel("Host(s) :");
        aLbl.setPreferredSize(new Dimension(pW, pH));
        hBox.add(Box.createGlue());
        hBox.add(aLbl);

        GBC.gridx = 0;
        GBC.gridy = 0;
        GBC.insets = new Insets(5, 5, 1, 5);
        this.add(hBox, GBC);

        hBox = Box.createHorizontalBox();
        hBox.add(this.cmbDivs);
        hBox.add(this.cmbHosts);
        hBox.add(new JLabel(" :: "));
        hBox.add(this.lblPort);

        GBC.gridx = 0;
        GBC.gridy = 1;
        GBC.insets = new Insets(1, 5, 1, 5);
        this.add(hBox, GBC);

        hBox = Box.createHorizontalBox();
        hBox.add(new JLabel("Country :"));
        hBox.add(Box.createHorizontalStrut(5));
        hBox.add(this.lblCountry);
        hBox.add(Box.createHorizontalGlue());
        hBox.add(new JLabel("Language :"));
        hBox.add(Box.createHorizontalStrut(5));
        hBox.add(this.lblLang);
        hBox.add(Box.createHorizontalGlue());
        hBox.setPreferredSize(new Dimension(pW * 2, pH));
        GBC.gridx = 0;
        GBC.gridy = 2;
        GBC.insets = new Insets(2, 5, 1, 5);
        this.add(hBox, GBC);

        hBox = Box.createHorizontalBox();
        hBox.add(new JLabel("Local :"));
        hBox.add(Box.createHorizontalStrut(5));
        hBox.add(this.lblLocal);
        hBox.add(Box.createHorizontalGlue());
        hBox.add(new JLabel("Version :"));
        hBox.add(Box.createHorizontalStrut(5));
        hBox.add(this.lblVer);
        hBox.add(Box.createHorizontalGlue());
        hBox.setPreferredSize(new Dimension(pW * 2, pH));
        GBC.gridx = 0;
        GBC.gridy = 3;
        GBC.insets = new Insets(2, 5, 1, 5);
        this.add(hBox, GBC);
    }

    public void setDivHosts(Map<String, List<String>> divHosts) {
        if (divHosts != null) {
            setDivisions(divHosts.keySet().toArray(String[]::new));
            String selectedDiv = (String) this.cmbDivs.getSelectedItem();
            List<String> hosts = divHosts.get(selectedDiv);
            if (hosts != null) setHosts(hosts);
            this.divHosts = divHosts;
        }
    }

    private void setHosts(List<String> hosts) {
        if (hosts != null) {
            this.cmbHosts.removeAllItems();
            hosts.forEach(this.cmbHosts::addItem);
        }
    }

    private void setDivisions(String[] divs) {
        if (divs != null) {
            this.cmbDivs.removeAllItems();
            for (String div : divs) {
                this.cmbDivs.addItem(div);
            }
        }
    }

    public void setPort(int port) { this.lblPort.setText(String.valueOf(port)); }
    public void setCountry(String country) { this.lblCountry.setText((country == null) ? "UNDEFINED" : country); }
    public void setLanguage(String lang) { this.lblLang.setText((lang == null) ? "UNDEFINED" : lang); }
    public void setGameLocal(int local) { this.lblLocal.setText(String.valueOf(local)); }
    public void setVersion(int ver) { this.lblVer.setText(String.valueOf(ver)); }
    public String getSelectedHost() {
        Object obj = this.cmbHosts.getSelectedItem();
        return (String) ((obj == null) ? "" : obj);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == this.cmbDivs && e.getStateChange() == ItemEvent.SELECTED) {
            String selectedDiv = (String) this.cmbDivs.getSelectedItem();
            if (this.divHosts != null) {
                List<String> hosts = this.divHosts.get(selectedDiv);
                if (hosts != null) setHosts(hosts);
            }
        }
    }
}
