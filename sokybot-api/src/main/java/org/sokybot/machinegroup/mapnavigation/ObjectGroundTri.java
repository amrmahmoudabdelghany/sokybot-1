package org.sokybot.machinegroup.mapnavigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sokybot.persistence.entities.geo.Vector2D;
import org.sokybot.persistence.entities.navmesh.ObjectGroundTriRef;
import org.sokybot.persistence.entities.navmesh.ObjectLineRef;
import org.sokybot.persistence.entities.navmesh.Position;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ObjectGroundTri {

	private CellObject cellObject;
	private ObjectGroundTriRef triangle;

	
	
	
	
	public ObjectLine[] getLines() { 
		
		//ObjectLine[] res = new ObjectLine[3] ; 
		 List<ObjectLine> res = new ArrayList<>() ; 
		int i = 0 ; 
		// get outlines  
		List<ObjectLineRef> lines = cellObject.objectNavMesh.outLines() ; 
		Set<Integer> indices = triangle.getOutLineIndex() ; 
		for(int index : indices ) { 
			ObjectLineRef lineRef = lines.get(index) ; 
			res.add(new ObjectLine(cellObject, lineRef)) ; 
		}
		
		// get inlines
		lines = cellObject.objectNavMesh.inLines() ; 
		indices = triangle.getInLineIndex() ; 
		
		for(int index : indices) { 
			ObjectLineRef lineRef = lines.get(index) ; 
			res.add(new ObjectLine(cellObject, lineRef)) ; 
		}
		
	//	if(res.isEmpty()) return  null ; 
		
		return  res.toArray(new ObjectLine[res.size()]) ; 
		
	}
	public Vector2D getPointA() {
		short pointA = triangle.getPointA();
		Position point = cellObject.objectNavMesh.getPointAt(pointA);
		Position objectPos = this.cellObject.getPosition();
		return  resolve(point.getX() , point.getY(), objectPos.getX(), objectPos.getY(), cellObject.getYAW());

	}

	public Vector2D getPointB() {
		short pointB = triangle.getPointB();
		Position point = cellObject.objectNavMesh.getPointAt(pointB);
		Position objectPos = this.cellObject.getPosition();
		return  resolve(point.getX() , point.getY(), objectPos.getX(), objectPos.getY(), cellObject.getYAW());
	}

	public Vector2D getPointC() {
		short pointC = triangle.getPointC();
		Position point = cellObject.objectNavMesh.getPointAt(pointC);
		Position objectPos = this.cellObject.getPosition();
		return resolve(point.getX() , point.getY(), objectPos.getX(), objectPos.getY(), cellObject.getYAW());

	}

	private Vector2D resolve(float  pointX , float pointY, float x, float y, float rotation) {


		float newX = (float) (pointX * Math.cos(-rotation) - -pointY * Math.sin(-rotation)); // resolve to object rotation and
																						// pos

		float newY = (float) (pointX * Math.sin(-rotation) + -pointY * Math.cos(-rotation));

		
		return new Vector2D(newX + x, newY + y) ; 
		
	}

}
