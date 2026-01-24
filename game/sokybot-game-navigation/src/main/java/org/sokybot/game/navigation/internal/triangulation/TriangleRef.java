package org.sokybot.game.navigation.internal.triangulation;


import org.sokybot.persistence.entities.geo.Vector2D;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TriangleRef {

	
	
	protected short pointAIndex ; 
	protected short pointBIndex ; 
	protected short pointCIndex ; 
	protected short lineAIndex ; 
	protected short lineBIndex ; 
	protected short lineCIndex ;

    public short getPointAIndex() { return pointAIndex; }
    public short getPointBIndex() { return pointBIndex; }
    public short getPointCIndex() { return pointCIndex; }

    public short getLineAIndex() { return lineAIndex; }
    public short getLineBIndex() { return lineBIndex; }
    public short getLineCIndex() { return lineCIndex; } 
	
	
		
	
	
	
}
