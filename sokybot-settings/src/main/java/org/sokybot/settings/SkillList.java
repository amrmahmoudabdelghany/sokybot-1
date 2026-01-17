package org.sokybot.settings;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SkillList {

	private int id ; 
	
	private List<String> skillList  = new ArrayList<>(); 
	
	private Settings settings ;
	
	public List<String> getSkillList() { return skillList; }
	
	public SkillList(Settings parent  ) {
		this.settings = parent ;   
	}
	
	public void add(String skill) { 
		if(skillList == null) { 
			this.skillList = new ArrayList<>() ; 
		}
		this.skillList.add(skill) ; 
	}
}
