/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sokybot.machinegroup.gamemodel;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 *
 * @author AMROO
 */
@Entity
@ToString
@NoArgsConstructor
public class SilkroadType  implements Serializable{

	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id ; 
	
	@ElementCollection(fetch = FetchType.EAGER)
	private Map<String , String > properties  ;  
     
    public SilkroadType(Map<String, String> props) {
    	 this.properties = props ; 
    }
    
    public String getLanguage() {
    	return this.properties.getOrDefault("Language", "UNKNOWN") ;
      //  return this.properties.get("Language") ; 
    }
    public String getCountry() {
        return this.properties.getOrDefault("Country" , "UNKNOWN"); 
    }
    
    public String getProperty(String propertyName) { 
    	return this.properties.get(propertyName) ; 
    }
    
    
}
