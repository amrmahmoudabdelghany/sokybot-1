package org.sokybot.machine.page.trainingpage;

import javax.annotation.PostConstruct;
import javax.swing.DefaultListModel;

import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.machinegroup.gamemodel.setting.TrainingArea;
import org.sokybot.machinegroup.gamemodel.setting.TrainingAreaSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component
public class AreaListModel extends DefaultListModel<String>{

	private TrainingAreaSettings settings ;
	
	private int activeIndex = -1 ; 
	
	
	@Autowired
	public AreaListModel(TrainingAreaSettings settings) {
		this.settings = settings ; 
	}
	
	@PostConstruct
	protected void init() { 
		String [] names =  settings.getTrainingAreaNames() ; 
		String active = settings.getActiveArea().getName() ; 
		
		for(int i = 0 ; i < names.length ; i++) { 
			if(names[i].equals(active)) { 
			   add(i , names[i] + "[Active]") ;	
			   activeIndex = i ; 
			}else
			add(i, names[i]);
		}
	}
	
	
//	public void addArea(TrainingArea area) { 
//		this.settings.addTrainingArea(area);
//		addElement(area.getName());
//	}
	
	@Override
	public String remove(int index) {
		
		if(index >= getSize()) throw new IndexOutOfBoundsException(index) ; 
		
		String val = get(index) ; 
		
		if(val.contains("[Active]")) { 
			throw new IllegalStateException("Could not remove active area") ; 
		}
		

		this.settings.removeTrainingArea(val);
		super.remove(index) ;
		
		for(int i = 0 ; i < getSize() ; i++) { 
			if(getElementAt(i).contains("[Active]")) { 
				this.activeIndex = i ; 
				break ; 
			}
 		}
		
		return val ; 
		
	}
	
	public int getActiveIndex() { 
		return this.activeIndex ; 
	}
	public void setActive(int index) { 
		
		if(index < getSize()  && index != this.activeIndex) { 
			
			String cActive = get(this.activeIndex) ; 
			cActive = cActive.substring(0 , cActive.indexOf("[")).trim() ;
			setElementAt(cActive, this.activeIndex); 
			
			cActive = get(index) ; 
			this.settings.setActiveArea(cActive);
			cActive += " [Active]" ; 
			setElementAt(cActive, index);
			this.activeIndex = index ; 
			
		}else { 
			System.out.println("Could not setActive at index " + index + " Active Index : " + this.activeIndex) ;
		}
	}
}
