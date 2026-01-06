package org.sokybot.machinegroup.mapnavigation;


import java.awt.geom.Rectangle2D;
import java.util.stream.Stream;

import org.sokybot.persistence.entities.geo.Vector2D;
import org.sokybot.persistence.entities.navmesh.NavCellRef;
import org.sokybot.persistence.entities.navmesh.NavObjectRef;
import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.entities.navmesh.Position;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Cell  {

	private Sector sector ; 
	private NavCellRef ref ; 
	
	
	
	
	public NavCellRef getRef() { 
		return this.ref ; 
	}
	
	
	
	@Deprecated
	public CellObject getCellObjectAt(int index) { 
		NavObjectRef ref = sector.getObjectRefAt(index) ;
		ObjectNavMesh objectNavMesh = sector.getNavMesh().getObjectNavMesh(ref.getObjId()) ;
		return new CellObject(this, ref, objectNavMesh) ; 
	}
	@Deprecated
	public Border getBorderAt(int index) { 
		return  new Border(this , sector.getBorderRefAt(index)) ; 
	}
	
	@Deprecated
	public CellLink getCellLinkAt(int index) { 
		return  new CellLink(this, sector.getCellLinkRefAt(index)) ; 
	}
	public Stream<Border> borders() { 
		return ref.borderRefs().map((borderRef)->new Border(this, borderRef)); 
	}
	
	public Stream<CellLink> links() { 
		return  ref.linkRefs().map((linkRef)->new CellLink(this , linkRef)) ; 
	}
	public Stream<CellObject> objects() { 
		return ref.cellObjectRefs().map((cellObjectRef)-> { 
			ObjectNavMesh objectNavMesh = sector.getNavMesh().getObjectNavMesh(cellObjectRef.getObjId()) ; 
		 	return new CellObject(this, cellObjectRef, objectNavMesh);
		 	}) ;
	}
	
	public int getUID() { 
		 return getSectorYX() << 16 | getIndex() ; 
	}
	public Vector2D getMin() { 
		Position min = this.ref.getMin() ; 
	    return new Vector2D(min.getX(), min.getY() ) ;
	}
	public Vector2D getMax() { 
		Position max = this.ref.getMax() ; 
		return new Vector2D(max.getX() , max.getY() ) ; 
	}
	public boolean hasObject() { 
		return this.ref.hasObject() ; 
	}
	public int getIndex() { 
		return this.ref.getIndex() ; 
	}
	public short getSectorYX() { 
		return this.sector.getSectorYX() ; 
	}
	
	public short getSectorX() {
		return (short) (getSectorYX() & 0xff);
	}

	public short getSectorY() {
		return (short) (getSectorYX() >> 8);
	}
	public Sector getSector() { 
		return this.sector ; 
	}
	
	public Rectangle2D.Float getRectangle() { 
		Position min = this.ref.getMin() ; 
		Position max = this.ref.getMax() ; 

		return  new Rectangle2D.Float(min.getX(), max.getY(), max.getX() - min.getX(), min.getY() - max.getY());

	}
	
	public int getBorderCount() { 
		return this.ref.getNavBorderIndex().size() ; 
	}
	public int getLinkCount() { 
		return this.ref.getNavCellLinkIndex().size() ; 
	}
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof Cell) ) return false ; 
		
		Cell otherCell = (Cell) other ; 
		
	  return sector.getSectorYX() == otherCell.getSectorYX() && getIndex() == otherCell.getIndex() ; 
	}
}
