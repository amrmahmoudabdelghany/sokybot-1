package org.sokybot.machinegroup.mapnavigation.triangulation;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Precision;
import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationAlgorithm;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.sets.ConstrainedPointSet;
import org.sokybot.machinegroup.gamemodel.geo.Edge2D;
import org.sokybot.machinegroup.gamemodel.geo.Vector2D;
import org.sokybot.machinegroup.gamemodel.navmesh.ObjectLineRef;
import org.sokybot.machinegroup.gamemodel.navmesh.ObjectNavMesh;
import org.sokybot.machinegroup.gamemodel.navmesh.Position;
import org.sokybot.machinegroup.mapnavigation.Border;
import org.sokybot.machinegroup.mapnavigation.Cell;
import org.sokybot.machinegroup.mapnavigation.CellLink;
import org.sokybot.machinegroup.mapnavigation.CellObject;
import org.sokybot.machinegroup.mapnavigation.ObjectLine;
import org.sokybot.machinegroup.mapnavigation.triangulation.CollisionDetector.CollisionResult;

public class Triangulator {

	private static Map<Integer, CellSplit> splitCache = new HashMap<>();

	private final CollisionDetector collisionDetector = new CollisionDetector();


	public CellSplit createSplite(Cell cell) {
		
		int key = (cell.getSectorYX() << 16) | cell.getIndex();

		if (splitCache.containsKey(key)) {

			return splitCache.get(key);
		} 



		CellSplitRef cellSplit = new CellSplitRef();

		List<TriangulationPoint> pointList = new ArrayList<>();
		List<ELine> lines = new ArrayList<>();

		float cellX1 = (float) cell.getMin().x;
		float cellY1 = (float) cell.getMin().y;
		float cellX2 = (float) cell.getMax().x;
		float cellY2 = (float) cell.getMax().y;
 
		Rectangle2D.Float rect = cell.getRectangle();
		
		if (rect.width < 0 || rect.height < 0) {
			throw new IllegalArgumentException();
		}

		if ((cell.getBorderCount() + cell.getLinkCount()) == 0)
			return null;

		cell.borders().map(ELine::new).forEach(lines::add);

		cell.links().map(ELine::new).forEach(lines::add);



		
		cell.objects()
		.filter((obj)->obj.isOutCanBlock() || obj.isOutCanBlock()) 
		.flatMap((obj)->obj.objectOutlines()) 
		.filter((outline)->outline.getLineFlag() != 16) 
		.forEach((line)->{
			Vector2D a = line.getPointA();
			Vector2D b = line.getPointB();
			byte index = 1;

			Vector2D[] colP = { new Vector2D(), new Vector2D() };
			CollisionResult r = CollisionResult.NO_COLLISION;

			r = collisionDetector.detectCollision(new Vector2D(cellX1, cellY1), new Vector2D(cellX2, cellY1), a,
					b, colP[index]);

			if (r == CollisionResult.COLLISION)
				index += -1;

			if (index >= 0 && r.value < CollisionResult.PARALLEL_OVERLAPPING.value) {
				r = collisionDetector.detectCollision(new Vector2D(cellX2, cellY1),
						new Vector2D(cellX2, cellY2), a, b, colP[index]);
				if (r == CollisionResult.COLLISION)
					index += -1;
			}

			if (index >= 0 && r.value < CollisionResult.PARALLEL_OVERLAPPING.value) {
				r = collisionDetector.detectCollision(new Vector2D(cellX2, cellY2),
						new Vector2D(cellX1, cellY2), a, b, colP[index]);
				if (r == CollisionResult.COLLISION)
					index += -1;
			}

			if (index >= 0 && r.value < CollisionResult.PARALLEL_OVERLAPPING.value) {
				r = collisionDetector.detectCollision(new Vector2D(cellX1, cellY2),
						new Vector2D(cellX1, cellY1), a, b, colP[index]);
				if (r == CollisionResult.COLLISION)
					index += -1;
			}

			if (index == 1) { // no Collision

				if (rect.contains(a.toFloat())) {
					if (line.getLineFlag() == 0) {
						lines.add(new ELine(a, b, LineType.Entry));
					} else {
						lines.add(new ELine(a, b, LineType.Block));
					}
				}

			} else if (index == 0) {

				if (rect.contains(a.toFloat())) {

					if (line.getLineFlag() == 0) {
						lines.add(new ELine(a, colP[1], LineType.Entry));
					} else {
						lines.add(new ELine(a, colP[1], LineType.Block));
					}

//					System.out.println("Add New Line") ; 
				} else if (rect.contains(b.toFloat())) {

					if (line.getLineFlag() == 0) {
						lines.add(new ELine(b, colP[1], LineType.Entry));
					} else {
						lines.add(new ELine(b, colP[1], LineType.Block));
					}

				} else {
					// System.out.println("Error") ;
					// throw new IllegalStateException();
				}
			} else if (index == -1) {

				if (line.getLineFlag() == 0) {
					lines.add(new ELine(colP[1], colP[0], LineType.Entry));
				} else {
					lines.add(new ELine(colP[1], colP[0], LineType.Block));
				}

			}


		});
		


		List<Integer> indices = new ArrayList<>();

		int cL1 = -1;
		int cL2 = 0;
		Vector2D newPoint = new Vector2D();

		CollisionResult result = CollisionResult.NO_COLLISION;

		do {
			cL1 += 1;
			if (cL1 == lines.size())
				break;

			cL2 = cL1 - 1;
			do {

				cL2 += 1;
				if (cL2 == lines.size())
					break;
				if (cL1 == cL2)
					continue;


				ELine line1 = lines.get(cL1);
				ELine line2 = lines.get(cL2);

				result = detectCollision(line1, line2, newPoint);

				if (result == CollisionResult.NO_COLLISION) {

					if ((line1.a.equals(line2.a) && line1.b.equals(line2.b))
							|| (line1.b.equals(line2.a) && line1.a.equals(line2.b))) {
						lines.remove(cL2);
						cL2--;
						continue;
					}

				} else if (result == CollisionResult.COLLISION) {

					lines.add(new ELine(newPoint.clone(), line1.a, line1.lineType));

					line1.a =  newPoint.clone();

					lines.add(new ELine(newPoint.clone(), line2.a, line2.lineType));

					line2.a = newPoint.clone();

				} else if (result == CollisionResult.POINT_ON_LINE) {

					if (!(Precision.equals(line1.a.x, newPoint.x, 0.001f)
							&& Precision.equals(line1.a.y, newPoint.y, 0.001f))
							&& !(Precision.equals(line1.b.x, newPoint.x, 0.001f)
									&& Precision.equals(line1.b.y, newPoint.y, 0.001f))) {

						lines.add(
								new ELine(newPoint.clone(), line1.b, line1.lineType));
						line1.b = newPoint.clone();

					}
					if (!(Precision.equals(line2.a.x, newPoint.x, 0.001f)
							&& Precision.equals(line2.a.y, newPoint.y, 0.001f))
							&& !(Precision.equals(line2.b.x, newPoint.x, 0.001f)
									&& Precision.equals(line2.b.y, newPoint.y, 0.001f))) {

						lines.add(
								new ELine(newPoint.clone(), line2.b, line2.lineType));
						line2.b = newPoint.clone();

					}
					/*
					 * if (!(line1.a.x == newPoint.x && line1.a.y == newPoint.y) && !(line1.b.x ==
					 * newPoint.x && line1.b.y == newPoint.y)) {
					 * 
					 * lines.add( new ELine(new Vector2D(newPoint.x, newPoint.y), line1.b,
					 * line1.lineType, line1.index)); line1.b = new Vector2D(newPoint.x,
					 * newPoint.y);
					 * 
					 * }
					 * 
					 * if (!(line2.a.x == newPoint.x && line2.a.y == newPoint.y) && !(line2.b.x ==
					 * newPoint.x && line2.b.y == newPoint.y)) {
					 * 
					 * lines.add( new ELine(new Vector2D(newPoint.x, newPoint.y), line1.b,
					 * line1.lineType, line1.index)); line2.b = new Vector2D(newPoint.x,
					 * newPoint.y);
					 * 
					 * }
					 */
				} else if (result == CollisionResult.PARALLEL_OVERLAPPING) {
					if (line1.a == line2.a && line1.b == line2.b || line1.b == line2.a && line1.a == line2.b) {
						lines.remove(cL2);
						cL2--;
						continue;
					}

					List<Vector2D> tmp = new ArrayList<>();
					tmp.add(line1.a);
					tmp.add(line1.b);
					tmp.add(line2.a);
					tmp.add(line2.b);

					tmp.sort(new Comparator<Vector2D>() {
						public int compare(Vector2D o1, Vector2D o2) {
							if (o1.x > o2.x) {
								return 1;
							} else if (o1.x < o2.x) {
								return -1;
							} else if (o1.y < o2.y) {
								return 1;
							} else if (o1.y > o2.y) {
								return -1;
							} else {
								return 0;
							}

						};
					});

					if (!tmp.get(1).equals(tmp.get(2))) {
						if (line1.lineType != line2.lineType) {
							// System.out.println("error");
						}

						if (!(tmp.get(0).equals(tmp.get(1)))) {
							lines.add(new ELine(tmp.get(0), tmp.get(1), line1.lineType));
						}
						if (!tmp.get(1).equals(tmp.get(2))) {
							lines.add(new ELine(tmp.get(1), tmp.get(2), line1.lineType));
						}
						if (!tmp.get(2).equals(tmp.get(3))) {
							lines.add(new ELine(tmp.get(2), tmp.get(3), line1.lineType));
						}

						lines.remove(cL1);

						if (cL1 < cL2) {
							lines.remove(cL2 - 1);

						} else {
							lines.remove(cL2);
						}

						cL1--;
						break;
					}

				}

			} while (true);

		} while (true);

		for (int i = 0; i < lines.size(); i++) {
			indices.add(addPoint(pointList, new PolygonPoint(lines.get(i).b.x, lines.get(i).b.y)));
			indices.add(addPoint(pointList, new PolygonPoint(lines.get(i).a.x, lines.get(i).a.y)));
		}

		int index[] = new int[indices.size()];
		for (int i = 0; i < index.length; i++)
			index[i] = indices.get(i);

		ConstrainedPointSet cps = new ConstrainedPointSet(pointList, index);

		
		try {
			Poly2Tri.triangulate(TriangulationAlgorithm.DTSweep, cps);
		} catch (Exception ex) {
			return null;
		}

		// check cast 
		cellSplit.points.addAll(cps.getPoints().stream().map((tp) -> new Point(tp.getX(), tp.getY())).collect(Collectors.toList()));

		List<DelaunayTriangle> triangles = cps.getTriangles();

		for (int i = 0; i < triangles.size(); i++) {

			
			DelaunayTriangle triangle = triangles.get(i);

			TriangulationPoint a = triangle.points[0];
			TriangulationPoint b = triangle.points[1];
			TriangulationPoint c = triangle.points[2];

			short aIndex = (short) cps.getPoints().indexOf(a);
			short bIndex = (short) cps.getPoints().indexOf(b);
			short cIndex = (short) cps.getPoints().indexOf(c);

			LineRef l1 = new LineRef(aIndex, bIndex);
			LineRef l2 = new LineRef(bIndex, cIndex);
			LineRef l3 = new LineRef(cIndex, aIndex);

			short line1Index = Short.MAX_VALUE;
			short line2Index = Short.MAX_VALUE;
			short line3Index = Short.MAX_VALUE;

			List<LineRef> lineRefs = cellSplit.lineRefs;

			for (short ii = 0; ii < lineRefs.size(); ii++) {
				if (l1.equals(lineRefs.get(ii))) {
					line1Index = ii;
				}
				if (l2.equals(lineRefs.get(ii))) {
					line2Index = ii;
				}
				if (l3.equals(lineRefs.get(ii))) {
					line3Index = ii;
				}

			}

			if (line1Index == Short.MAX_VALUE) {
				lineRefs.add(l1);
				line1Index = (short) (lineRefs.size() - 1);
			}

			if (line2Index == Short.MAX_VALUE) {
				lineRefs.add(l2);
				line2Index = (short) (lineRefs.size() - 1);
			}

			if (line3Index == Short.MAX_VALUE) {
				lineRefs.add(l3);
				line3Index = (short) (lineRefs.size() - 1);
			}

			Point lA = cellSplit.points.get(l1.aIndex);
			Point lB = cellSplit.points.get(l1.bIndex);

			if (isBlockedLine(lA.getX(), lA.getY(), lB.getX(), lB.getY())) {
				l1.lineType = org.sokybot.machinegroup.mapnavigation.triangulation.CellSplitRef.LineType.Block;
			}

			lA = cellSplit.points.get(l2.aIndex);
			lB = cellSplit.points.get(l2.bIndex);

			if (isBlockedLine(lA.getX(), lA.getY(), lB.getX(), lB.getY())) {
				l2.lineType = org.sokybot.machinegroup.mapnavigation.triangulation.CellSplitRef.LineType.Block;
			}

			lA = cellSplit.points.get(l3.aIndex);
			lB = cellSplit.points.get(l3.bIndex);

			if (isBlockedLine(lA.getX(), lA.getY(), lB.getX(), lB.getY())) {
				l3.lineType = org.sokybot.machinegroup.mapnavigation.triangulation.CellSplitRef.LineType.Block;
			}

			
			cellSplit.lineRefs.get(line1Index).neighbors.add(line2Index);
			cellSplit.lineRefs.get(line1Index).neighbors.add(line3Index);

			cellSplit.lineRefs.get(line2Index).neighbors.add(line1Index);
			cellSplit.lineRefs.get(line2Index).neighbors.add(line3Index);

			cellSplit.lineRefs.get(line3Index).neighbors.add(line1Index);
			cellSplit.lineRefs.get(line3Index).neighbors.add(line2Index);

			// if (!lineRefs.contains(l1)) {
			// lineRefs.add(l1);
			// }

			// if (!lineRefs.contains(l2)) {
			// lineRefs.add(l2);
			// }
			// if (!lineRefs.contains(l3)) {
			// lineRefs.add(l3);
			// }

			// short l1Index = (short) cellSplit.lineRefs.indexOf(l1);
			// short l2Index = (short) cellSplit.lineRefs.indexOf(l2);
			// short l3Index = (short) cellSplit.lineRefs.indexOf(l3);

			// l1 = lineRefs.get(l1Index);
			// l2 = lineRefs.get(l2Index);
			// l3 = lineRefs.get(l3Index);

			TriangleRef triRef = new TriangleRef();

			triRef.pointAIndex = aIndex;
			triRef.pointBIndex = bIndex;
			triRef.pointCIndex = cIndex;

			triRef.lineAIndex = line1Index;
			triRef.lineBIndex = line2Index;
			triRef.lineCIndex = line3Index;

			cellSplit.triangleRefs.add(triRef);

		}

		for (int i = 0; i < lines.size(); i++) {

			ELine line = lines.get(i);

			Vector2D la = line.a;
			Vector2D lb = line.b;
			
			if(la.equals(lb)) { 
				System.out.println("Line has the same point") ; 
			}
			
			int ii1 = -1 ; 
			int ii2 = -1 ;
			int ii = 0 ; 
			
			for(Point p : cellSplit.points) { 
				
				if(Precision.equals(p.getX(), la.x, 0.001f) && Precision.equals(p.getY(), la.y, 0.001f)) { 
					ii1 = ii ; 
				}
				if(Precision.equals(p.getX(), lb.x, 0.001f) && Precision.equals(p.getY(), lb.y, 0.001f)) { 
					ii2 = ii ; 
				}
				ii++ ; 
				
				if(ii1 != -1 && ii2 != -1) break ; 
			}
			

			if (ii1 == -1 || ii2 == -1)
				throw new IllegalStateException();

			if (ii1 == ii2)
				// throw new IllegalStateException();
				System.out.println("error");

			for (LineRef lineRef : cellSplit.lineRefs) {

				if ((lineRef.aIndex == ii1 && lineRef.bIndex == ii2) || 
						(lineRef.bIndex == ii1 && lineRef.aIndex == ii2)) {

					// if (lineRef.lineType != org.sokybot.common.astar.CellSplit.LineType.NonBlock)
					// continue;

					switch (line.lineType) {

					case BC:

						lineRef.lineType = org.sokybot.machinegroup.mapnavigation.triangulation.CellSplitRef.LineType.BC;
						break;

					case Block:
						lineRef.lineType = org.sokybot.machinegroup.mapnavigation.triangulation.CellSplitRef.LineType.Block;
						break;

					case Entry:
						lineRef.lineType = org.sokybot.machinegroup.mapnavigation.triangulation.CellSplitRef.LineType.ObjectEntrance;
						break;

					case LC:
						lineRef.lineType = org.sokybot.machinegroup.mapnavigation.triangulation.CellSplitRef.LineType.LC;
						break;

					}

					// lineRef.extInfo =(short) line.index ;

				}
			}

		}

		CellSplit res =  new CellSplit(cellSplit);
		
		splitCache.put(key, res);

		return res ; 
	}

	public boolean isBlockedLine(double ax, double ay, double bx, double by) {
		double dx = ax - bx;
		double dy = ay - by;

		return Math.abs(Math.sqrt(dx * dx + dy * dy)) < 2;

	}

	private int addPoint(List<TriangulationPoint> pointList, PolygonPoint point) {

		int c = 0;

		for (TriangulationPoint p : pointList) {
			if (p.getX() == point.getX() && p.getY() == point.getY())
				return c;
			else
				c++;
		}

		pointList.add(point);
		return c;

	}

	private Point toPoint(Position point) {

		return new Point(point.getX(), point.getY());
	}

//	private Point resolve(Point a, float x, float y, float rotation) {
//		Point res = new Point(a.x , a.y) ; 
//		res.x = (float) (res.x * Math.cos(-rotation) - -res.y * Math.sin(-rotation)); // resolve to object rotation and pos
//		res.x += x;
//
//		res.y = (float) (res.x * Math.sin(-rotation) + -res.y * Math.cos(-rotation));
//		res.y += y;
//		return res ; 
//
//	}

	private Vector2D resolve(float pointX, float pointY, float x, float y, float rotation) {

		// System.out.println("Point Before Resolve : " + a + " x " + x + " , y " + y +
		// " r " + rotation) ;

		float newX = (float) (pointX * Math.cos(-rotation) - -pointY * Math.sin(-rotation)); // resolve to object
																								// rotation and pos

		float newY = (float) (pointX * Math.sin(-rotation) + -pointY * Math.cos(-rotation));

		return new Vector2D(newX + x, newY + y);
	}

	private CollisionResult detectCollision(ELine l1, ELine l2, Vector2D colPoint) {

		return this.collisionDetector.detectCollision(l1.a, l1.b, l2.a, l2.b, colPoint);

	}

	public class ELine extends Edge2D {

		// public int index = 0;
		public LineType lineType;

		public ELine(CellLink link) {
			super(link.getMin(), link.getMax());
			this.lineType = link.hasNeighbour() ? LineType.LC : LineType.Block;

		}

		public ELine(Border border) {
			super(border.getMin(), border.getMax());
			this.lineType = border.hasNeighbour() ? LineType.BC : LineType.Block;

		}

		public ELine(Vector2D a, Vector2D b, LineType lineType) {
			super(a, b);
			this.lineType = lineType;
		}
	}

}
