package org.sokybot.machinegroup.gamemodel.setting;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.sokybot.persistence.entities.MonsterType;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class SkillList {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id ; 
	
	//@GeneratedValue(strategy = GenerationType.IDENTITY)
	//@Enumerated(EnumType.STRING)
	
	//MonsterType monsterType  ; 
	
	@ElementCollection(fetch = FetchType.EAGER)
	@Cascade(CascadeType.ALL)
	private List<String> skillList  = new ArrayList<>(); 
	
	
	@ManyToOne
	private Settings settings ;
	
	
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
