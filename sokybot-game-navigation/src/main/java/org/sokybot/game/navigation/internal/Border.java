package org.sokybot.game.navigation.internal;

import org.sokybot.persistence.entities.geo.Vector2D;
import org.sokybot.persistence.entities.navmesh.NavBorderRef;
import org.sokybot.persistence.entities.navmesh.Position;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Border {


	private Cell cell ; 
	
	protected NavBorderRef borderRef ; 

	
	
	public Cell getNeighbourCell() { 
		Sector parentSector = this.cell.getSector() ; 
		NavMesh navMesh = parentSector.getNavMesh() ; 
		
		if(parentSector.getSectorYX() == this.borderRef.getRegionSourceYX()) { 
		  Sector destSector = 	navMesh.getSector(this.borderRef.getRegionDestionationYX()) ;
		  return destSector.getCellAt(this.borderRef.getCellDestinationIndex()) ;
		}else { 
			Sector sourceSector = navMesh.getSector(this.borderRef.getRegionSourceYX()) ;
			return sourceSector.getCellAt(this.borderRef.getCellSourceIndex()) ; 
			//return new Cell(sourceSector , sourceSector.getCellAt(this.borderRef.getCellSourceIndex())) ; 
		}
	}
	
	
	public int getIndex() { 
		return cell.getSector().indexOf(this) ; 
	}
	
	public Vector2D getMin() { 
		return borderRef.getMin().toVector2D() ;
	}
	public Vector2D getMax() { 
		return borderRef.getMax().toVector2D() ; 
	}
	public boolean hasNeighbour() { 
		return this.borderRef.hasNeighbour() ; 
	}
	public Cell getCell() { 
		return this.cell ; 
	}
	
}
