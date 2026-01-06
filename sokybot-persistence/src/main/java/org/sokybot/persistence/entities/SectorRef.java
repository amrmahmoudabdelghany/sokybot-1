package org.sokybot.persistence.entities;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.sokybot.persistence.entities.navmesh.NavBorderRef;
import org.sokybot.persistence.entities.navmesh.NavCellLinkRef;
import org.sokybot.persistence.entities.navmesh.NavCellRef;
import org.sokybot.persistence.entities.navmesh.NavObjectRef;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Builder
@EqualsAndHashCode
@ToString
@NoArgsConstructor
//@AllArgsConstructor
public class SectorRef implements Serializable {

	@Id
	private short sectorYX;

	// private transient NavMesh navMesh ;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@JoinColumn(name = "segment")
	private List<NavObjectRef> navObjects;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@JoinColumn(name = "segment")
	private List<NavCellRef> navCells;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@JoinColumn(name = "segment")
	private List<NavBorderRef> navBorders;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@JoinColumn(name = "segment")
	private List<NavCellLinkRef> navCellLinks;

	private float[] hightMap;
	
	public short getSectorYX() {
		return sectorYX;
	}
	
	public List<NavObjectRef> getNavObjects() {
		return navObjects;
	}
	
	public List<NavCellRef> getNavCells() {
		return navCells;
	}
	
	public List<NavBorderRef> getNavBorders() {
		return navBorders;
	}
	
	public List<NavCellLinkRef> getNavCellLinks() {
		return navCellLinks;
	}
	
	public float[] getHightMap() {
		return hightMap;
	}

	
	
	// public void setNavMesh(NavMesh navMesh) {
	// this.navMesh = navMesh ;
	// }

	public Stream<NavCellRef> cellRefs() { 
		return this.navCells.stream() ; 
	}
	
	public Stream<NavBorderRef> borderRefs() { 
		return this.navBorders.stream() ; 
	}
	
	public Stream<NavCellLinkRef> linkRefs() { 
		return this.navCellLinks.stream() ; 
	}
	public Stream<NavObjectRef> objectRefs() { 
		return this.navObjects.stream() ; 
	}
	
	
	@Deprecated
	public NavCellRef getCellAt(int index) {
		return this.navCells.get(index);
		// return navMesh.getNavCell(Integer.toHexString(this.sectorYX & 0xffff), index)
		// ;
	}


	@Deprecated
	public NavCellLinkRef getCellLinkAt(int index) {
		return this.navCellLinks.get(index);

		// return navMesh.getNavCellLink(Integer.toHexString(this.sectorYX & 0xffff),
		// index) ;
	}


	@Deprecated
	public NavObjectRef getObjectAt(int index) {
		return this.navObjects.get(index);
		// return navMesh.getNavObject(Integer.toHexString(this.sectorYX & 0xffff),
		// index) ;
	}


	@Deprecated
	public NavBorderRef getBorderAt(int index) {
		return this.navBorders.get(index);
		// return navMesh.getNavBorder(Integer.toHexString(this.sectorYX & 0xffff),
		// index) ;
	}
	

	@Deprecated
	public int indexOf(NavCellRef cell) { 
		return this.navCells.indexOf(cell) ; 
	}

	public int getCellCount() {
		return this.navCells.size();
		/// return this.navMesh.getNavCellCount(Integer.toHexString(this.sectorYX &
		/// 0xffff)) ;
	}

	public SectorRef(short sectorYX, List<NavObjectRef> navObjects, List<NavCellRef> navCells, List<NavBorderRef> navBorders,
			List<NavCellLinkRef> navCellLinks, float[] hightMap) {
		super();
		this.sectorYX = sectorYX;
		this.navObjects = navObjects;
		
		this.navCells = navCells;
		for(NavCellRef cellRef :  navCells) { 
			cellRef.sectorRef = this ; 
		}
		this.navBorders = navBorders;
		this.navCellLinks = navCellLinks;
		this.hightMap = hightMap;
	}

	

}
