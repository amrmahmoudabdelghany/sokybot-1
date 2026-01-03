package org.sokybot.machine.page.trainingpage;

import java.awt.Dimension;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.noos.xing.mydoggy.TabbedContentManagerUI.TabLayout;
import org.sokybot.machine.IMachinePage;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.machinegroup.gamemodel.setting.TrainingArea;
import org.sokybot.machinegroup.gamemodel.setting.TrainingAreaSettings;
import org.sokybot.utils.SwingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import info.clearthought.layout.TableLayout;

/*
 * TableLayout 
 * http://www.clearthought.info/sun/products/jfc/tsc/articles/tablelayout/Simple.html
 */
@Component
public class AreaTab extends JPanel {
 
	
	@Autowired
	private AreaList  areaList ; 

	
	@PostConstruct
	private void init() { 
		
		double border = 5 ; 
		
		double size[][] = {
				{border , TableLayout.FILL , border} , //cols
				{border , TableLayout.PREFERRED , border } // rows
				};
		
		setLayout(new TableLayout(size));
		add(this.areaList , "1 ,1 ") ; 
		this.areaList.setBorder(BorderFactory.createTitledBorder("Training Area"));		
		
	}
	
	
	
	
	public static void main(String args[]) { 
	LocationBox box  = new LocationBox(new Trainer(), new TrainingArea()) ; 
		
	JPanel content = new JPanel() ; 
	double border = 5 ; 
	
	double size[][] = {
			{ border ,TableLayout.FILL , TableLayout.FILL} , 
			{ border ,TableLayout.PREFERRED }
			};
	
	content.setLayout(new TableLayout(size));
	
	content.add(box , "1 ,1 ") ; 
	
	box.setBorder(BorderFactory.createTitledBorder("Training Area"));		

	JFrame frame  = new JFrame() ; 
	frame.add(content) ; 
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
	frame.pack();  
	frame.setPreferredSize(new Dimension(400 , 400));
	frame.setLocationRelativeTo(null);
	frame.setVisible(true); ; 

	}

	
	
}
