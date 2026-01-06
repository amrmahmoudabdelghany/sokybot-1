package org.sokybot.machinegroup.mapnavigation;

import org.sokybot.persistence.entities.geo.Vector2D;
import org.sokybot.persistence.entities.navmesh.NavCellLinkRef;
import org.sokybot.persistence.entities.navmesh.Position;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CellLink {

	
	private Cell cell ; 
	protected NavCellLinkRef cellLinkRef ;
	
	
	
	
	
	public Cell getNeighbourCell() { 
		
		if(cellLinkRef.getCellSourceIndex() == cell.getIndex()) { 
			return this.cell.getSector().getCellAt(cellLinkRef.getCellDestinationIndex()) ;
		}else { 
			return this.cell.getSector().getCellAt(cellLinkRef.getCellSourceIndex()) ; 
		}
	
	}
	
	public int getIndex() { 
		return cell.getSector().indexOf(this) ; 
	}
	public  Vector2D getMin() { 
		return this.cellLinkRef.getMin().toVector2D() ;
	}
	public Vector2D getMax() { 
		return this.cellLinkRef.getMax().toVector2D() ; 
	}
	public boolean hasNeighbour() { 
		return this.cellLinkRef.hasNeighbour() ; 
	}
	public Cell getCell() { 
		return this.cell ; 
	}
	
	
}
