package org.sokybot.game.navigation.internal;

import java.util.List;
import java.util.stream.Stream;

import org.sokybot.persistence.entities.navmesh.NavBorderRef;
import org.sokybot.persistence.entities.navmesh.NavCellLinkRef;
import org.sokybot.persistence.entities.navmesh.NavObjectRef;
import org.sokybot.persistence.entities.navmesh.Position;
import org.sokybot.persistence.entities.SectorRef;

public class Sector {

	
	private NavMesh navMesh ; 
	
	private SectorRef sectorRef ;

	public Sector(NavMesh navMesh, SectorRef sectorRef) {
		super();
		this.navMesh = navMesh;
		this.sectorRef = sectorRef;
	} 
	
	public short getSectorYX() { 
		return this.sectorRef.getSectorYX() ; 
	}
	public short getSectorX() {
		return (short) (getSectorYX() & 0xff);
	}

	public short getSectorY() {
		return (short) (getSectorYX() >> 8);
	}
	public NavMesh getNavMesh() { 
		return this.navMesh ; 
	}

	@Deprecated
	public Cell getCellAt(int index) { 
		return new Cell(this, this.sectorRef.getCellAt(index)) ; 
	}
	
	public Stream<Cell> cells() { 
		return this.sectorRef.cellRefs().map((cellRef)->new Cell(this, cellRef)) ; 
	}

	@Deprecated
	public int indexOf(Border border) { 
	   return  sectorRef.getNavBorders().indexOf(border.borderRef) ;
	}

	@Deprecated
	public int indexOf(CellLink link) { 
		return sectorRef.getNavCellLinks().indexOf(link.cellLinkRef) ;
	}
	
	@Deprecated
	public int indexOf(CellObject cellObject) { 
		return sectorRef.getNavObjects().indexOf(cellObject.objectRef) ; 
	}
	
	@Deprecated
	protected NavObjectRef getObjectRefAt(int index) { 
		return sectorRef.getObjectAt(index) ; 
	}
	@Deprecated
	protected NavBorderRef getBorderRefAt(int index) { 
		return sectorRef.getBorderAt(index) ; 
	}
	
	@Deprecated
	protected NavCellLinkRef getCellLinkRefAt(int index) { 
		return sectorRef.getCellLinkAt(index) ; 
	}
	
	public Cell getCellAt(int sectorXOffset , int sectorYOffset ) { 
	 return	this.sectorRef.cellRefs()
		.filter((cell)->{
			
			Position min = cell.getMin();
			Position max = cell.getMax();

			if (min.getX() <= sectorXOffset && max.getX() >= sectorXOffset) {
				if (max.getY() <= 192 - sectorYOffset && min.getY() >= 192 - sectorYOffset) {
					return true ; 
				}
			}
			return false ; 
		}).map(ref->new Cell(this, ref)).findFirst().orElseThrow() ;
	}
	
	
	
}
