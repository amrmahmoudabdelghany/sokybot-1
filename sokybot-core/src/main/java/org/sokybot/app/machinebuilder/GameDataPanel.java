package org.sokybot.app.machinebuilder;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.sing_group.gc4s.visualization.VisualizationUtils;
import org.sokybot.machinegroup.gamemodel.Division;
import org.sokybot.machinegroup.gamemodel.DivisionInfo;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarkLaf;

@Component
@Scope("prototype")
public class GameDataPanel extends JPanel implements ItemListener {

	public static final String HOSTS_PROP = "hosts";
	public static final String DIVISIONS_PROP = "divisions";

	private JComboBox<String> cmbHosts;
	private JComboBox<String> cmbDivs;
	private JLabel lblPort;
	private JLabel lblVer;
	private JLabel lblCountry;
	private JLabel lblLang;
	private JLabel lblLocal;

	private Map<String, List<String>> divHosts;

	public GameDataPanel() {

		this.cmbHosts = new JComboBox<String>();
		this.cmbDivs = new JComboBox<String>();
		this.lblPort = new JLabel("5000");
		this.lblVer = new JLabel("320");
		this.lblCountry = new JLabel("Egypt");
		this.lblLang = new JLabel("Arabic");
		this.lblLocal = new JLabel("22");

	}

	@PostConstruct
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
		// put strut here
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

		// review this statement after first display
		// hBox.setPreferredSize(new Dimension(300, hBox.getHeight()));

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

		
		if(divHosts != null) { 
			
			setDivisions(divHosts.keySet().toArray(String[]::new));
			String selectedDiv = (String) this.cmbDivs.getSelectedItem();
			List<String> hosts = divHosts.get(selectedDiv) ; 
			if(hosts != null) { 
				setHosts(hosts) ; 
			}
			
			this.divHosts = divHosts ; 
		}
		

	}

	public String[] getCurrentDivisionsOrNull() {
		int size = this.cmbDivs.getItemCount();

		if (size == 0)
			return null;

		String[] cDivs = new String[size];

		for (int i = 0; i < size; i++) {
			cDivs[i] = this.cmbDivs.getItemAt(i);
		}

		return cDivs;

	}

	private void setHosts(List<String> hosts) {

		if (hosts != null) {
			this.cmbHosts.removeAllItems();
			hosts.forEach((h) -> this.cmbHosts.addItem(h));
		}

	}

	private void setDivisions(String[] divs) {
		if (divs != null) {

			// String[] old = getCurrentDivisionsOrNull();
			this.cmbDivs.removeAllItems();

			for (String div : divs) {
				this.cmbDivs.addItem(div);
			}

			// this.changeSupport.firePropertyChange(DIVISIONS_PROP, old, divs);
		}
	}

	public void setPort(int port) {
		this.lblPort.setText(String.valueOf(port));
	}

	public void setCountry(String country) {
		this.lblCountry.setText((country == null) ? "UNDEFINED" : country);
	}

	public void setLanguage(String lang) {
		this.lblLang.setText((lang == null) ? "UNDEFINED" : lang);
	}

	public void setGameLocal(int local) {
		this.lblLocal.setText(String.valueOf(local));
	}

	public void setVersion(int ver) {
		this.lblVer.setText(String.valueOf(ver));
	}

	public String getSelectedHost() {
		Object obj = this.cmbHosts.getSelectedItem() ; 
		
		return (String)((obj== null) ? "" : obj);
	}

	public static void main(String args[]) {
		FlatDarkLaf.setup();
		GameDataPanel panel = new GameDataPanel();
		panel.init();
		
		System.out.println(panel.getSelectedHost()) ; 

		VisualizationUtils.showComponent(panel);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {

		if (e.getSource() == this.cmbDivs) {

			if (e.getStateChange() == ItemEvent.SELECTED) {

				String selectedDiv = (String) this.cmbDivs.getSelectedItem();
				if (this.divHosts != null) {
					//Division div = this.divInfo.getDivision(selectedDiv);
					List<String> hosts = this.divHosts.get(selectedDiv) ; 
					if (hosts != null) {
						setHosts(hosts);

					}
				}
			}

		}
	}
}
