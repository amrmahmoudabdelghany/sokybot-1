package org.sokybot.machine.page.trainingpage;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.settings.Settings;
import org.sokybot.settings.TrainingArea;
import org.sokybot.settings.TrainingAreaSettings;
import org.sokybot.utils.SwingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.extras.components.FlatTextField;

import info.clearthought.layout.TableLayout;

@Component
@Scope("prototype")
public class LocationBox extends JPanel  {

	
	protected final FlatTextField txtX;
	protected final FlatTextField txtY;
	protected final FlatTextField txtR;

	private final JButton btnGetCord ; 

	//private TrainingAreaSettings areaSettings ; 
	
	private Trainer trainer ; 
	
	private TrainingArea area ; 
	
	
	public LocationBox(Trainer trainer ,  TrainingArea area) {
		
		
		this.area = area ; 
		this.txtX = new FlatTextField();
		this.txtY = new FlatTextField();
		this.txtR = new FlatTextField();
		
	//	this.areaSettings = userConfig ; 
	//	this.selectedArea = userConfig.getActiveArea().getName()  ; 
		this.trainer = trainer ; 
		
		this.btnGetCord = new JButton("Get Current");
		init();
	}

	private void init() {
		
		this.txtX.setText(String.valueOf(area.getAreaX())) ; 
		this.txtY.setText(String.valueOf(area.getAreaY())) ; 
		this.txtR.setText(String.valueOf(area.getAreaR())) ; ;
		
		//BoxLayout layout = new BoxLayout(this, BoxLayout.PAGE_AXIS);
		//setLayout(layout);
		
		this.txtX.setColumns(10);
		this.txtY.setColumns(10);
		this.txtR.setColumns(10);
		
		
		double border = 5 ; 
		
		double size[][] = {
				{border , TableLayout.PREFERRED ,
					TableLayout.PREFERRED , border ,
					TableLayout.PREFERRED  , TableLayout.PREFERRED ,
					border, TableLayout.PREFERRED,
					border} , //cols
				{border , TableLayout.PREFERRED ,border ,TableLayout.PREFERRED , border } // rows
				};
		setLayout(new TableLayout(size)); 
		
		add(new JLabel("X : ") , "1 , 1") ; 
		add(this.txtX , "2 , 1") ; 
		
		add(new JLabel("Y : ") , "4 , 1") ; 
		add(this.txtY , "5 ,1") ; 
		
		add(this.btnGetCord , "7 , 1") ; 
		
		add(new JLabel("R : ") , "1 , 3") ; 
		add(this.txtR , "2 , 3") ; 
		
		
	//	setLayout(new GridBagLayout());
	//	GridBagConstraints gbc = new GridBagConstraints() ; 
		
	//	gbc.gridx = 0 ; 
	//	gbc.gridy = 0 ; 
		
	//	JPanel line = new JPanel() ; 
	//	line.add(new JLabel("X :")) ;
	//	line.add(this.txtX) ; 
		
		//add(line , gbc) ; 
		
		//gbc.gridx = 1 ; 
		//gbc.gridy = 0 ; 
	
	//	line = new JPanel() ; 
	//	line.add(new JLabel("Y :")) ; 
	//	line.add(this.txtY) ; 
		
	//	add(line , gbc) ; 
		
	//	gbc.insets = new Insets(0, 0 , 0, 5) ; 
	//	gbc.gridx = 2 ; 
	//	gbc.gridy = 0 ; 
		
		//add(this.btnGetCord , gbc) ; 
		//gbc.insets = new Insets(0, 2 , 0, 0) ; ; 
		//gbc.gridx = 0 ; 
		//gbc.gridy = 1 ; 
		
		//line = new JPanel() ; 
		//line.add(new JLabel("R :"));
		//line.add(this.txtR) ; 
		
		//add(line , gbc) ; 
		
		InputVerifier iv = new InputVerifier() {
			
			@Override
			public boolean verify(JComponent input) {
				  FlatTextField field = (FlatTextField) input ; 
				  if(!NumberUtils.isParsable(field.getText()) ) { 
					  field.setOutline("error") ; 
					  return false ;
				  }else { 
					  field.setOutline(null);
				  }
				 
				
				 return true ; 
			}
		}; 
		
		this.txtX.setInputVerifier(iv); 
		this.txtY.setInputVerifier(iv);
		this.txtR.setInputVerifier(iv);
		
		
		FocusListener fListener = new FocusAdapter() {
		
			String txtOld ; 
			@Override
			public void focusGained(FocusEvent e) {
				FlatTextField field =(FlatTextField) e.getSource() ; 
				
				txtOld = field.getText() ; 
				
			}
			@Override
			public void focusLost(FocusEvent e) {
				FlatTextField field =(FlatTextField) e.getSource() ; 
				String txt = field.getText() ; 
				if(!txt.equals(txtOld) && NumberUtils.isParsable(txt)) {
				if(field == txtX) { 
				
					area.setAreaX(Integer.parseInt(txt)) ;
				}else if(field == txtY) { 
					area.setAreaY(Integer.parseInt(txt)) ;
				}else if(field == txtR) { 
					area.setAreaR( Integer.parseInt(txt)) ; 					
				}
				}
			}
		};
		
		this.txtX.addFocusListener(fListener);
		this.txtY.addFocusListener(fListener);
		this.txtR.addFocusListener(fListener);
		
		this.btnGetCord.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				
				int x = trainer.getX() ; 
				int y = trainer.getY(); 
			
				txtX.setText(String.valueOf(x)) ;
				txtY.setText(String.valueOf(y)) ;
				
				if(x != area.getAreaX() ||
						y != area.getAreaY()) {
					area.setAreaX( x); 
					area.setAreaY( y); 
				}
				
				
			}

		
		}
		);
		
		
		//		Box line = Box.createHorizontalBox() ; 
//		
//		line.add(new JLabel("X :")) ;
//		line.add(Box.createHorizontalStrut(5)) ; 
//		line.add(this.txtX) ; 
//		
//		line.add(Box.createHorizontalGlue()) ; 
//		
//		line.add(new JLabel("Y :")) ;
//		line.add(Box.createHorizontalStrut(5)) ; 
//		line.add(this.txtY) ; 
//		
//		line.add(Box.createHorizontalStrut(5)) ; 
//		line.add(this.btnGetCord) ; 
//		
//		
//		add(line) ; 
//		add(Box.createVerticalStrut(5)) ; 
//		
//		line = Box.createHorizontalBox() ; 
//		
//		line.add(new JLabel("Radius :")) ; 
//		line.add(Box.createHorizontalStrut(5)) ; 
//		line.add(this.txtR) ; 
//		line.add(Box.createGlue()) ; 
//		
//		add(line) ; 
//		add(Box.createVerticalStrut(5)) ; 
//		
	}
	
	
//	public void setSelectedArea(String selectedArea) { 
//		this.selectedArea = selectedArea ; 
//
//		this.txtX.setText(String.valueOf(areaSettings.getArea(selectedArea).getAreaX()));
//		this.txtY.setText(String.valueOf(areaSettings.getArea(selectedArea).getAreaY()));
//		this.txtR.setText(String.valueOf(areaSettings.getArea(selectedArea).getAreaR()));
//
//	}
	
	public int getAreaX() { 
		return Integer.parseInt(this.txtX.getText()) ;
	}
	public int getAreaY() { 
		return Integer.parseInt(this.txtY.getText()) ;
		
	}
	public int getAreaR() {
		return Integer.parseInt(this.txtR.getText()) ;
		
	}
	
//	public void setAreaX(int x ) { 
//		this.txtX.setText(String.valueOf(x));
//	}
//	public void setAreaY(int y ) { 
//		this.txtY.setText(String.valueOf(y));
//	}
//	public void setAreaR(int r ) { 
//		this.txtR.setText(String.valueOf(r));
//	}
	public static void main(String args[]) { 
		FlatDarculaLaf.setup() ;
		TrainingAreaSettings config = new TrainingAreaSettings() ; 
		config.getArea("Active").setAreaX(2); 
		config.getArea("Active").setAreaY( 20) ; 
		config.getArea("Active").setAreaR( 30);
		Trainer t = new Trainer() ; 
		t.setLocation(40, 50);
		
		//SwingUtils.display(new LocationBox(t , config));
		
		
		
	}

}
