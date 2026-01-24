package org.sokybot.game.navigation.internal.triangulation;

import java.util.stream.Stream;

import org.sokybot.persistence.entities.geo.Vector2D;

import lombok.AllArgsConstructor;

public class Triangle {

	private CellSplitRef cellSplitRef ; 
	private TriangleRef triRef;

    public Triangle(CellSplitRef cellSplitRef, TriangleRef triRef) {
        this.cellSplitRef = cellSplitRef;
        this.triRef = triRef;
    }

	
	public Vector2D getPointA() {
		Point p = cellSplitRef.points.get(triRef.getPointAIndex());
		return new Vector2D(p.getX(), p.getY());
	}

	public Vector2D getPointB() {

		Point p = cellSplitRef.points.get(triRef.getPointBIndex());
		return new Vector2D(p.getX(), p.getY());
	}

	public Vector2D getPointC() {
		Point p = cellSplitRef.points.get(triRef.getPointCIndex());
		return new Vector2D(p.getX(), p.getY());
	}

	
	public Stream<Line> lines() { 
		return Stream.of(getLineA() , getLineB() , getLineC() ) ; 
	}
	
	public Line getLineA() { 
	     LineRef lineRef = cellSplitRef.lineRefs.get(triRef.getLineAIndex()) ; 
	     return new Line(cellSplitRef , lineRef) ;
	}
	public Line getLineB() { 
	     LineRef lineRef = cellSplitRef.lineRefs.get(triRef.getLineBIndex()) ; 
	     return new Line(cellSplitRef , lineRef) ;
	}
	public Line getLineC() { 
	     LineRef lineRef = cellSplitRef.lineRefs.get(triRef.getLineCIndex()) ; 
	     return new Line(cellSplitRef , lineRef) ;
	}
	
	
    public boolean contains(Vector2D point) {
    	
    	Vector2D a = getPointA() ; 
    	Vector2D b = getPointB() ; 
    	Vector2D c = getPointC() ; 
    	
        double pab = point.sub(a).cross(b.sub(a));
        double pbc = point.sub(b).cross(c.sub(b));

        if (!hasSameSign(pab, pbc)) {
            return false;
        }

        double pca = point.sub(c).cross(a.sub(c));

        if (!hasSameSign(pab, pca)) {
            return false;
        }

        return true;
    }

    

    /**
     * Tests if the two arguments have the same sign.
     * 
     * @param a
     *            The first floating point argument
     * @param b
     *            The second floating point argument
     * @return Returns true iff both arguments have the same sign
     */
    private boolean hasSameSign(double a, double b) {
        return Math.signum(a) == Math.signum(b);
    }
}
