package org.sokybot.machinegroup.mapnavigation.triangulation;

import java.util.stream.Stream;

import org.sokybot.model.geo.Vector2D;
import org.sokybot.machinegroup.mapnavigation.triangulation.CellSplitRef.LineType;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Line {
	
	
	private CellSplitRef cellSplitRef ; 
	private LineRef lineRef ; 

	
	
	
	@Deprecated
	public int getIndex() { 
		return cellSplitRef.lineRefs.indexOf(lineRef) ; 
	}
	public LineType getLineType() { 
		return this.lineRef.lineType ; 
	}
	public Stream<Line> neighbors(){ 
		return lineRef.neighbors.stream().map((index)->new Line(cellSplitRef, cellSplitRef.lineRefs.get(index))) ; 
	}
	public boolean isNotBlock() { 
		return lineRef.lineType != CellSplitRef.LineType.Block ; 
	}
	public Vector2D getPointA() { 
		Point p  =  cellSplitRef.points.get(lineRef.getAIndex()) ; 
		return new Vector2D(p.getX(), p.getY()) ;
	}
	
	public Vector2D getPointB() { 
		Point p  =  cellSplitRef.points.get(lineRef.getBIndex()) ; 
		return new Vector2D(p.getX(), p.getY()) ;
	}
	
}
