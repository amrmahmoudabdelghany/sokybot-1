package org.sokybot.game.navigation.internal.triangulation;

import java.util.stream.Stream;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CellSplit {

	
	private CellSplitRef cellSplitRef ;
	
	
	
	public Stream<Line> lines() { 
		return this.cellSplitRef.lineRefs.stream().map((lineRef)->new Line(cellSplitRef, lineRef)) ; 
	}
	public Stream<Triangle> triangles() { 
		return this.cellSplitRef.triangleRefs().map((triRef)->new Triangle( cellSplitRef , triRef)) ;
	}
	
}
