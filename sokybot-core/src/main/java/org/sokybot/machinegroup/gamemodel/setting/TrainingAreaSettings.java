package org.sokybot.machinegroup.gamemodel.setting;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.apache.commons.lang3.builder.ToStringExclude;
import org.springframework.context.annotation.Bean;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Entity
@NoArgsConstructor
public class TrainingAreaSettings {

	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private int id ; 
	
	@OneToOne
	@JoinColumn(name = "setting_id")
	private Settings settings ; 
	
	

	@OneToMany(cascade = CascadeType.ALL , fetch =  FetchType.EAGER , mappedBy = "areaSettings")
//	@JoinTable(name = "setting_area_mapping" ,
//	joinColumns = {@JoinColumn(name = "area_setting_id" , referencedColumnName = "id")} , 
	//inverseJoinColumns = {@JoinColumn(name = "area_id" , referencedColumnName = "id" )})
	@MapKey(name = "name")
	private Map<String, TrainingArea> areas  = new HashMap<>(); 
	
	
	@Column(name = "ACTIVE_AREA" ,nullable =  false )
	private String activeArea ; 

	
	public TrainingAreaSettings(Settings settings , String trainerName) { 
		this.settings = settings ; 
		TrainingArea defaultArea = getArea(trainerName);
		this.activeArea = defaultArea.getName() ; 

		System.out.println(" Default Training Area is : " + this.activeArea) ;
	
	}
	
	
	
	public TrainingArea removeTrainingArea(String name) { 
		if(name == null || name.isBlank() )
			throw new IllegalArgumentException("Training Area Name is not valid ") ; 
		
		if(!this.areas.containsKey(name)) 
			throw new IllegalArgumentException("'" + name + "' is  not a training area") ; 
		
		if(name.equals(this.activeArea)) 
			throw new IllegalArgumentException("Could not remove active area") ; 
		
		return this.areas.remove(name) ; 
		
		
	}
	
	public boolean containsAreaName(String name) { 
		return this.areas.containsKey(name) ; 
	}
	
//	public int getActiveAreaX() { 
//		return this.areas.get(activeArea).getAreaX() ;
//	}
//	public int getActiveAreaY() { 
//		return this.areas.get(activeArea).getAreaY() ;
//	}
//	public int getActiveAreaR() { 
//		return this.areas.get(activeArea).getAreaR() ;
//	}
//
//	public int getAreaX(String name) { 
//		return getArea(name).getAreaX() ; 
// 	}
//	
//	public int getAreaY(String name) { 
//		return getArea(name).getAreaY() ; 
//	}
//	
//	public int getAreaR(String name) { 
//		return getArea(name).getAreaR() ; 
//	}
//	
//	public void setAreaX(String name , int x) { 
//		 getArea(name).setAreaX(x) ; 
// 	}
//	
//	public void setAreaY(String name , int y) { 
//		 getArea(name).setAreaY(y) ; 
//	}
//	public void setAreaR(String name , int r) { 
//		 getArea(name).setAreaR(r) ; 
//	}
//	
	
	public TrainingArea getArea(String name) { 
		if(name == null || name.isBlank() )
			throw new IllegalArgumentException("Training Area Name is not valid ") ; 
		
		
		if(!this.areas.containsKey(name))  {
			TrainingArea x =  new TrainingArea(this, name, 0, 0, 0) ; 
			this.areas.put(x.getName(), x) ; 
			return x ; 
		}
			//throw new IllegalArgumentException("'" + name + "' is  not a training area") ; 
		
		return this.areas.get(name) ;
	}
	
	public TrainingArea getActiveArea() { 
		
		return getArea(this.activeArea) ;
	}
	public String[] getTrainingAreaNames() { 
		return this.areas.keySet().toArray((n)->new String[n]) ; 
	}
	
 	public String getActiveTrainingArea() { 
		return this.activeArea ; 
	}
 	

 	public boolean isActiveArea(TrainingArea area) { 
 		return this.activeArea.equals(area.getName()) ; 
 	}
	
	public void setActiveArea(String name) { 
		if(name == null || name.isBlank() )
			throw new IllegalArgumentException("Training Area Name is not valid ") ; 
		
		
		if(!this.areas.containsKey(name)) 
			throw new IllegalArgumentException("'" + name + "' is  not a training area") ; 
		
		this.activeArea = name ; 
		
	}
	
	
	
//	public void addTrainingArea(TrainingArea area) { 
//		String name = area.getName() ; 
//		
//		if(name == null || name.isBlank() )
//			throw new IllegalArgumentException("Training Area Name is not valid ") ; 
//		
//		
//		if(this.areas.containsKey(name)) 
//			throw new IllegalArgumentException("Training Area Name should be unieq") ; 
//		
//		
//		this.areas.put(name, area) ; 
//		
//	}

	
}
