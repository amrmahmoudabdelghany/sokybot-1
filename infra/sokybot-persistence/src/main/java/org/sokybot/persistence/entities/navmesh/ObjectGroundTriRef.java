package org.sokybot.persistence.entities.navmesh;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
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
	
	public int getId() { return id; }
	public short getPointA() { return pointA; }
	public short getPointB() { return pointB; }
	public short getPointC() { return pointC; }
	public short getUnk() { return unk; }
	public Set<Integer> getInLineIndex() { return inLineIndex; }
	public Set<Integer> getOutLineIndex() { return outLineIndex; }
	
	public void setId(int id) { this.id = id; }
	public void setPointA(short pointA) { this.pointA = pointA; }
	public void setPointB(short pointB) { this.pointB = pointB; }
	public void setPointC(short pointC) { this.pointC = pointC; }
	public void setUnk(short unk) { this.unk = unk; }
	public void setInLineIndex(Set<Integer> inLineIndex) { this.inLineIndex = inLineIndex; }
	public void setOutLineIndex(Set<Integer> outLineIndex) { this.outLineIndex = outLineIndex; }
	
	
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
