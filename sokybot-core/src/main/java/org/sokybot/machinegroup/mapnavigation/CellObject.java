package org.sokybot.machinegroup.mapnavigation;

import java.util.List;
import java.util.stream.Stream;

import org.sokybot.machinegroup.gamemodel.navmesh.NavObjectRef;
import org.sokybot.machinegroup.gamemodel.navmesh.ObjectNavMesh;
import org.sokybot.machinegroup.gamemodel.navmesh.Position;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CellObject {

	private Cell cell;
	protected NavObjectRef objectRef;
	protected ObjectNavMesh objectNavMesh;

	
	
	public int getRegionId() { 
		return this.objectRef.getRegionID() ;
	}
	public int getObjectId() { 
		return this.objectRef.getObjId() ;
	}
	public Stream<ObjectLine> entryLines() { 
		return objectOutlines().filter((outline)->outline.isEntryLine()) ; 
	}
	public Stream<ObjectLine> objectLines() { 
		return Stream.concat(objectOutlines(),objectInlines()) ; 
	}
	public Stream<ObjectLine> objectOutlines() {
		return this.objectNavMesh.outLines().stream().map((line) -> new ObjectLine(this, line));
	}
	public Stream<ObjectLine> objectInlines() { 
		return this.objectNavMesh.inLines().stream().map((line)-> new ObjectLine(this , line)) ; 
	}

	public Stream<ObjectLine> passableLines() { 
		return Stream.concat(objectInlines().filter((l)->l.isPassable()), entryLines()) ; 
	}
	public Stream<ObjectGroundTri> objectGroundTriangles() { 
		return this.objectNavMesh.triangles().stream().map((tri)->new ObjectGroundTri(this , tri)) ; 
	}
	
	public ObjectGroundTri getTriangleAt(int index) { 
		return new ObjectGroundTri(this, this.objectNavMesh.triangles().get(index)) ; 
	}
	public ObjectNavMesh getNavMesh() {
		return this.objectNavMesh;
	}

	public Position getPosition() {
		return this.objectRef.getPosition();
	}
	
	public boolean isOutCanBlock() { 
		return this.objectNavMesh.isOutCanBlock() ;
	}
	public boolean isInCanBlock() { 
		return this.objectNavMesh.isInCanBlock() ; 
	}

	public boolean hasEntrance() {
		return this.objectNavMesh.isHasEntrance();
	}

	public float getYAW() {
		return objectRef.getYaw();
	}

	public Cell getCell() {
		return this.cell;
	}

	public int getIndex() {
		return this.cell.getSector().indexOf(this);
	}
}
