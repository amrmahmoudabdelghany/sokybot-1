package org.sokybot.machinegroup.gamemodel.navmesh;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NavCellRef implements Serializable{

	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id ; 
	
	
	protected SectorRef sectorRef ; 
	
	private Position min ; 
	private Position max ; 
	
	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<Short> navObjectIndex ;  


	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<Short> navBorderIndex ; 


	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<Short> navCellLinkIndex ;
	
	
	
	public boolean hasObject() { 
		return this.navObjectIndex.size() > 0 ; 
	}



	public NavCellRef(Position min, Position max, List<Short> navObjectIndex, List<Short> navBorderIndex,
			List<Short> navCellLinkIndex) {
		super();
		this.min = min;
		this.max = max;
		this.navObjectIndex = navObjectIndex;
		this.navBorderIndex = navBorderIndex;
		this.navCellLinkIndex = navCellLinkIndex;
	}
	
	
	

	public short getIndex() { 
		return (short) this.sectorRef.indexOf(this) ; 
	}
	public short getSectorYX() { 
		return this.sectorRef.getSectorYX() ; 
	}
	public Stream<NavCellLinkRef> linkRefs() { 
		return this.navCellLinkIndex.stream().map((sectorRef::getCellLinkAt)) ; 
	}
	
	public Stream<NavBorderRef> borderRefs() { 
		return this.navBorderIndex.stream().map(sectorRef::getBorderAt) ; 
	}
	public Stream<NavObjectRef> cellObjectRefs() { 
		return this.navObjectIndex.stream().map(sectorRef::getObjectAt) ;
	}
	
}
