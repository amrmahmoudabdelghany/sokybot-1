package org.sokybot.machinegroup.gamemodel.navmesh;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ObjectGroundTriRef implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	private short pointA;
	private short pointB;
	private short pointC;
	private short unk;

	
	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@JoinColumn(name = "nav_inline")
	private Set<Integer> inLineIndex = new HashSet<>() ; 
	

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@JoinColumn(name = "nav_outline")
	private Set<Integer> outLineIndex = new HashSet<>(); 
	
	
	
	public ObjectGroundTriRef(short pointA, short pointB, short pointC, short unk) {
		super();
		this.pointA = pointA;
		this.pointB = pointB;
		this.pointC = pointC;
		this.unk = unk;
	}

	
	public void addInLineNeighbour(int index) { 
			if(index >= 0)
			this.inLineIndex.add(index) ; 
	}
	public void addOutLineNeighbour(int index) { 
		if(index >= 0) 
			this.outLineIndex.add(index) ; 
	}

//	public void addNeighbour(short lineIndex) {
////		this.lineIndex[unk] = lineIndex;
//	//	this.unk += 1;
//	}

}