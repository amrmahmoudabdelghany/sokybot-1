package org.sokybot;

import org.apache.commons.math3.util.Precision;
import org.sokybot.machinegroup.gamemodel.geo.Vector2D;

public class CollisionDetector {

	
	
	
	
	
	protected CollisionDetector() {} 
	
	
	
	
	
	protected CollisionResult detectCollision(Vector2D p1, Vector2D p2,
			Vector2D p3, Vector2D p4, Vector2D collPoint) {

		double dx1 = p2.x - p1.x;
		double dy1 = p2.y - p1.y;
		double dx2 = p4.x - p3.x;
		double dy2 = p4.y - p3.y;

		double denom = dy2 * dx1 - dx2 * dy1;

		if (denom == 0) {

			if (p1.x * (p2.y - p3.y) + p2.x * (p3.y - p1.y) + p3.x * (p1.y - p2.y) == 0) {

				if ((p1.x >= p3.x && p1.x <= p4.x) ||
					(p1.x <= p3.x && p1.x >= p4.x) ||
					(p2.x >= p3.x && p2.x <= p4.x) ||
					(p2.x <= p3.x && p2.x >= p4.x) || 
					(p3.x >= p1.x && p3.x <= p2.x) ||
						 (p3.x <= p1.x && p3.x >= p2.x)) {

					if ((p1.y >= p3.y && p1.y <= p4.y) ||
						(p1.y <= p3.y && p1.y >= p4.y) ||
					    (p2.y >= p3.y && p2.y <= p4.y) || 
					    (p2.y <= p3.y && p2.y >= p4.y) ||
					    (p3.y >= p1.y && p3.y <= p2.y) || 
					    (p3.y <= p1.y && p3.y >= p2.y)) {

						return CollisionResult.PARALLEL_OVERLAPPING;
					}
				}

				return CollisionResult.NO_COLLISION;
			}

			return CollisionResult.NO_COLLISION;
		} else {

			double ua = ((dx2 * (p1.y - p3.y)) - (dy2 * (p1.x - p3.x))) / denom;
			double ub = ((dx1 * (p1.y - p3.y)) - (dy1 * (p1.x - p3.x))) / denom;

			if (ua < 0 || ua > 1 || ub < 0 || ub > 1) {
				return CollisionResult.NO_COLLISION;
			} else {
				
				
				
				collPoint.x = (float) Precision.round(p1.x + (ua * (p2.x - p1.x)), 4);
				collPoint.y = (float) Precision.round(p1.y + (ua * (p2.y - p1.y)), 4) ; 
				
				if (equalsPoints(collPoint , p3) || equalsPoints(collPoint , p4) || equalsPoints(collPoint , p1) || equalsPoints(collPoint , p2)) {
					return CollisionResult.POINT_ON_LINE;
				}

				return CollisionResult.COLLISION;
			}

		}

	}

	
	
	private boolean equalsPoints(Vector2D p1 , Vector2D p2 ) { 
		return Precision.equals(p1.x, p2.x, 0.001f) && Precision.equals(p1.y, p2.y, 0.001f);
		//return p1.x == p2.x && p1.y == p2.y ; 
	}

	
	protected enum CollisionResult {

		COLLISION(-1), NO_COLLISION(0), POINT_ON_LINE(2), PARALLEL_OVERLAPPING(1);

		int value;

		private CollisionResult(int val) {
			this.value = val;
		}

	}
	
	
}
