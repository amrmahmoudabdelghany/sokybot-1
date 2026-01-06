package org.sokybot.persistence.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
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
	
	public int getId() { return id; }
	public Settings getSettings() { return settings; }
	public Map<String, TrainingArea> getAreas() { return areas; }
	public String getActiveArea() { return activeArea; }
	
	public void setId(int id) { this.id = id; }
	public void setSettings(Settings settings) { this.settings = settings; }
	public void setAreas(Map<String, TrainingArea> areas) { this.areas = areas; }
	// setActiveArea is custom below
	

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
	
	public TrainingArea getActiveAreaInstance() { 
		
		return getArea(this.activeArea) ;
	}
	public String[] getTrainingAreaNames() { 
		return this.areas.keySet().toArray((n)->new String[n]) ; 
	}
	
 	public String getActiveTrainingArea() { 
		return this.activeArea ; 
	}
 	
 	public Set<String> getAreaNames() {
 		return this.areas.keySet();
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

}
