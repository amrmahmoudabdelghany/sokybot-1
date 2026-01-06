package org.sokybot.machinegroup.mapnavigation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Precision;
import org.sokybot.persistence.entities.geo.Vector2D;
import org.sokybot.persistence.entities.navmesh.ObjectLineRef;
import org.sokybot.persistence.entities.navmesh.Position;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
public class ObjectLine {

	private CellObject cellObject;
	protected ObjectLineRef lineRef;

    public ObjectLine(CellObject cellObject, ObjectLineRef lineRef) {
        this.cellObject = cellObject;
        this.lineRef = lineRef;
    }

	public CellObject getCellObject() {
		return this.cellObject;
	}

	public boolean hasNeighbour() {
		return this.lineRef.hasNeighbour();
	}

	public int getId() {
		return this.lineRef.getId() ; 
	}
	public int getIndex() { 
		 List<ObjectLineRef> lines = 	cellObject.objectNavMesh.outLines() ;

		  if(lines.contains(lineRef)) { 
			  return lines.indexOf(lineRef);
		  }
		  
		  lines = cellObject.objectNavMesh.inLines() ; 
		  

		  if(lines.contains(lineRef)) { 
			  return lines.indexOf(lineRef);
		  }
		  
		  System.out.println("ObjectLine index == -1") ; 
		  
		  return -1 ; 
		  
	}
	public Vector2D getPointA() {
		Position pointA = cellObject.objectNavMesh.getPointAt(lineRef.getPointAIndex());
		Position objectPos = cellObject.getPosition();
		return resolve(pointA.getX() , pointA.getY(), objectPos.getX(), objectPos.getY(), cellObject.getYAW());
	}

	public Vector2D getPointB() {
		Position pointB = cellObject.objectNavMesh.getPointAt(lineRef.getPointBIndex());
		Position objectPos = cellObject.getPosition();
		return resolve(pointB.getX() , pointB.getY(), objectPos.getX(), objectPos.getY(), cellObject.getYAW());
	}

	public Vector2D getMidPoint() { 

		Vector2D pointA = getPointA() ; 
		Vector2D pointB = getPointB() ; 
		
	//	pointA.x = (-135 + this.cellObject.getCell().getSectorX()) * 192 + ((float) pointA.x); 
	//	pointA.y =  (-92 + this.cellObject.getCell().getSectorY()) * 192 + (192 - ((float) pointA.y));
		
		//pointB.x = (-135 + this.cellObject.getCell().getSectorX()) * 192 + ((float) pointB.x); 
	//	pointB.y =  (-92 + this.cellObject.getCell().getSectorY()) * 192 + (192 - ((float) pointB.y));
		
		float dx = (float) (pointA.x - pointB.x);
		float dy = (float) (pointA.y - pointB.y);
	   
		pointA.x -= (dx /2) ; 
		pointA.y -= (dy /2) ; 
		
		Vector2D res =  new Vector2D(pointA.x, pointA.y) ;
		
		res.x =  Precision.round(res.x, 3) ;
		res.y =  Precision.round(res.y, 3) ;
		return res ; 
 	}
	
	public Cell getNeighbourCell() { 
		 return null ; 
	}
	
	public List<ObjectLine> getTriangleEdgs() { 
		List<ObjectLine> res = new ArrayList<>();

		ObjectGroundTri tri = getNeighbourTriangleA();

		if (tri != null) {
			ObjectLine[] triLines = tri.getLines();
			for (ObjectLine line : triLines) {
				if (!line.equals(this))
					res.add(line);
				
			}
		}
		
		tri = getNeighbourTriangleB() ; 
		if (tri != null) {
			ObjectLine[] triLines = tri.getLines();
			for (ObjectLine line : triLines) {
				if (!line.equals(this))
					res.add(line);
				
			}
		}

		return res ; 
	}
	public boolean isBlockedOutline() {
		return  this.cellObject
			.objectOutlines()
			.filter((outline) -> !outline.isEntryLine())
			.anyMatch((outline) -> {

				Vector2D aa = outline.getPointA();
				Vector2D bb = outline.getPointB();
				Vector2D taa = this.getPointA();
				Vector2D tbb = this.getPointB();

				return ((Precision.equals(aa.x, taa.x, 1f) && Precision.equals(aa.y, taa.y, 1f)
						&& Precision.equals(bb.x, tbb.x, 1f) && Precision.equals(bb.y, tbb.y, 1f)))
						|| ((Precision.equals(aa.x, tbb.x, 1f) && Precision.equals(aa.y, tbb.y, 1f)
								&& Precision.equals(bb.x, taa.x, 1f)
								&& Precision.equals(bb.y, taa.y, 1f)));
			}); 
		 
	}
	public List<ObjectLine> getNeighbours() {


		
		List<ObjectLine> res = new ArrayList<>() ; 
		 
		
		this.cellObject
		.passableLines()
		.forEach((inline)->{
			if(!this.equals(inline)) { 
				Vector2D a = this.getPointA();
				Vector2D b = this.getPointB();
				Vector2D ta = inline.getPointA();
				Vector2D tb = inline.getPointB();

				boolean r =  (Precision.equals(a.x, ta.x, 1f) && Precision.equals(a.y, ta.y, 1f)
						|| Precision.equals(b.x, tb.x, 1f) && Precision.equals(b.y, tb.y, 1f))
						|| (Precision.equals(a.x, tb.x, 1f) && Precision.equals(a.y, tb.y, 1f)
			|| Precision.equals(b.x, ta.x, 1f) && Precision.equals(b.y, ta.y, 1f));

				if(r) { 
					boolean r2 = inline.isBlockedOutline() ; 


					if (!r2)
						res.add(inline);
				}
			}
		});
		
		
		return res ;
	}

	public boolean isEntryLine() { 
		return this.lineRef.getFlag() == 0 ; 
	}
	public boolean isPassable() { 
		byte flag = this.lineRef.getFlag() ; 
		
	  return (flag  == 0 || flag == 16 || flag == 8 || flag == 4 || flag == 20 ) ; 	
	}
	
	public ObjectGroundTri getNeighbourTriangleA() {
		int index = lineRef.getNeighbourAIndex();
		if (index == 65535)
			return null;
		return this.cellObject.getTriangleAt(index);
	}

	public ObjectGroundTri getNeighbourTriangleB() {
		int index = lineRef.getNeighbourBIndex();
		if (index == 65535)
			return null;
		return this.cellObject.getTriangleAt(index);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ObjectLine))
			return false;

		ObjectLine other = (ObjectLine) obj;

		return this.lineRef.equals(other.lineRef);
	}
//	public Vector2D getNeighbourPointA() {
//		
//		int index = lineRef.getNeighbourAIndex() ; 
//		if(index == 65535) return null ; 
//		
//		Point nPointA = cellObject.objectNavMesh.getPointAt(index);
//		Position objectPos = cellObject.getPosition();
//
//		nPointA = resolve2(nPointA, objectPos.getX(), objectPos.getY(), cellObject.getYAW());
//		return new Vector2D(nPointA.x, nPointA.y);
//	}

//	public Vector2D getNeighbourPointB() {
//		int index = lineRef.getNeighbourBIndex() ; 
//		if(index == 65535) return null ; 
//		
//		Point nPointB = cellObject.objectNavMesh.getPointAt(index);
//		Position objectPos = cellObject.getPosition();
//		nPointB = resolve2(nPointB, objectPos.getX(), objectPos.getY(), cellObject.getYAW());
//		return new Vector2D(nPointB.x, nPointB.y);
//	}

	public byte getLineFlag() {
		return this.lineRef.getFlag();
	}

	private Vector2D resolve(float pointX , float pointY, float x, float y, float rotation) {



		float newX = (float) (pointX * Math.cos(-rotation) - -pointY * Math.sin(-rotation)); // resolve to object rotation and
																						// pos

		float newY = (float) (pointX * Math.sin(-rotation) + -pointY * Math.cos(-rotation));


		return new Vector2D(newX + x , newY + y);

	}

}
