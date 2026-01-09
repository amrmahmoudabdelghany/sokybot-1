package org.sokybot.game.navigation.internal.triangulation;

import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.sokybot.model.geo.Triangle2D;
import org.sokybot.persistence.entities.geo.Vector2D;

public class CellSplitRef {

	
	public static enum LineType { 
		
		Block((byte)0) ,
		NonBlock((byte)1) , 
		LC ((byte)2), 
		BC ((byte)3), 
		ObjectEntrance((byte)4)  ; 
		
		public byte value ; 
		
		private LineType(byte val) { 
			this.value = val ; 
		}
	}

	
	
	
	public List<Point> points = new ArrayList<>() ; 
	public List<LineRef> lineRefs = new ArrayList<>() ; 
	public List<TriangleRef> triangleRefs = new ArrayList<>(); 
	
	
	
	
	public Stream<TriangleRef> triangleRefs() { 
		return this.triangleRefs.stream() ; 
	}
	
	
}
