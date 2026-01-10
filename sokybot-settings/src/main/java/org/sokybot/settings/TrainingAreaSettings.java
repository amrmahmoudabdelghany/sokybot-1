package org.sokybot.settings;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringExclude;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@NoArgsConstructor
public class TrainingAreaSettings {

	private int id ; 
	
	private Settings settings ; 
	
	private Map<String, TrainingArea> areas  = new HashMap<>(); 
	
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
	
	public TrainingArea getArea(String name) { 
		if(name == null || name.isBlank() )
			throw new IllegalArgumentException("Training Area Name is not valid ") ; 
		
		
		if(!this.areas.containsKey(name))  {
			TrainingArea x =  new TrainingArea(this, name, 0, 0, 0) ; 
			this.areas.put(x.getName(), x) ; 
			return x ; 
		}

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
	
}
