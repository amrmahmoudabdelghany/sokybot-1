package org.sokybot.machinegroup.mapnavigation;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.math3.util.Precision;

import org.sokybot.persistence.entities.geo.Vector2D;
import org.sokybot.persistence.entities.navmesh.ObjectLineRef;
import org.sokybot.persistence.entities.navmesh.NavBorderRef;
import org.sokybot.persistence.entities.navmesh.NavCellRef;
import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.entities.navmesh.Position;
import org.sokybot.machinegroup.mapnavigation.triangulation.CellSplit;
import org.sokybot.machinegroup.mapnavigation.triangulation.CellSplitRef;
import org.sokybot.machinegroup.mapnavigation.triangulation.LineRef;
import org.sokybot.machinegroup.mapnavigation.triangulation.CellSplitRef.LineType;
import org.sokybot.machinegroup.mapnavigation.triangulation.Line;
import org.sokybot.machinegroup.service.ISroMaterialDAO;

import static org.sokybot.utils.SilkroadUtils.getSectorYX;
import static org.sokybot.utils.SilkroadUtils.getSectorOffset;

public class RuteFinder {

	private ISroMaterialDAO sroDao;

	private Position pStart, pStop;

	private CellSplit cellSplit;

	private short pStartZone, pStopZone;

	private Map<Long, Node> oList = new LinkedHashMap<>();
	private Map<Long, Node> cList = new LinkedHashMap<>();

	private List<Long> dList = new ArrayList<>();

	private NavMesh navMesh;
	private long lastNode = -1;

	private boolean pathFound = false;

	private float hEstimate = 1;

	public RuteFinder(ISroMaterialDAO sroDao) {
		this.sroDao = sroDao;
		this.navMesh = new NavMesh(sroDao);

	}

	public List<Vector2D> findPath(float startX, float startY, float stopX, float stopY, float h) {
		return findPath(new Position(startX, 0, startY), new Position(stopX, 0, stopY), h);
	}

	public List<Vector2D> findPath(Position start, Position stop, float h) {

		System.out.println("findPath start (" + start.getX() + "," + start.getY() + " )" + " , stop (" + stop.getX()
				+ "," + stop.getY() + ")");
		List<Vector2D> res = new ArrayList<>();

		hEstimate = h;

		this.oList.clear();
		this.cList.clear();
		this.dList.clear();
		this.pStart = start;
		this.pStop = stop;

		this.pathFound = false;

		findStart();
		findStop();

		if (this.oList.size() == 0 || this.dList.size() == 0) {
			throw new IllegalStateException("Start/Stop not found");
		}

		do {

			if (this.oList.size() == 0) {
				throw new IllegalStateException("Target not reachable");
			}

		} while (process(getNextNodeId()));

		Node node = this.cList.get(this.lastNode);

		Float[] points;
		do {

			points = getPoints(node);

			float dx = points[0].x - points[1].x;
			float dy = points[0].y - points[1].y;
			points[0].x -= dx / 2;
			points[0].y -= dy / 2;

			Vector2D v = new Vector2D(points[0].x, points[0].y);
			v.x = Precision.round(v.x, 3);
			v.y = Precision.round(v.y, 3);

			res.add(v);

			if (node.parent == 0) {
				break;
			}
			node = this.cList.get(node.parent);
		} while (true);

		return res;

	}

	private boolean process(long parentId) {

		Node parentNode = this.oList.get(parentId);

		this.oList.remove(parentId);
		this.cList.put(parentId, parentNode);

		for (long dId : this.dList) {

			if (dId == parentId) {
				this.lastNode = parentId;
				return false;
			}
		}

		// System.out.println("ParentNode id : " + parentNode.getId());
		List<Node> neighBors = getNeighbors(parentNode);

		if (neighBors.isEmpty()) {
			return true;
		}

		for (Node n : neighBors) {

			long nId = n.getId();

			if (cList.containsKey(nId)) {
				continue;
			}

			n.g = parentNode.g + getG(parentNode, n);

			if (oList.containsKey(nId)) {
				if (n.g < oList.get(nId).g) {
					n.h = oList.get(nId).g;
					n.parent = parentId;
					this.oList.put(nId, n);
				}
			} else {
				n.parent = parentId;
				n.h = getH(n);
				this.oList.put(nId, n);

			}

		}

		return true;
	}

	static int c = 0;

	private List<Node> getNeighbors(Node node) {
		// System.out.println("getNeighbors from " + node.getId()) ;
		// System.out.println("GetNeighbors from node of type " + node.nodeType.name());
		List<Node> list = new ArrayList<>();
		switch (node.nodeType) {

		case CELL_BORDER:

			Border border = (Border) node.object;

			return getNeighborsFromCell(border.getNeighbourCell(), border.getCell());
		case CELL_LINK:

			CellLink link = (CellLink) node.object;
			if (link.hasNeighbour())
				return getNeighborsFromCell(link.getNeighbourCell(), link.getCell());
			else
				break;
		case aTriEdge:
			return getNeighborsFromCellSplit(node);
		// break;
		case OBJECT_LINE:

			return getNeighborsFromObjectLine(node);
		// throw new UnsupportedOperationException() ;

		case NotSet:
			break;
		// case aObject:
		// break;
		default:
			break;

		}

		return list;
	}

	private long getNextNodeId() {
		long id = 0;
		long flow = 0xffffffffl;
		Collection<Node> nodes = this.oList.values();

		for (Node n : nodes) {

			int v = n.g + n.h;
			if (v < flow) {
				flow = v;
				id = n.getId();

			}

		}

		return id;
	}

	private void findStop() {

		List<Node> nods = getNeighborsFromPosition(pStop);
		this.dList = new ArrayList<>();
		nods.forEach((node) -> {
			this.dList.add(node.getId());
		});
	}

	private List<Node> getNeighborsFromPosition(Position p) {
		List<Node> nList = new ArrayList<>();

		float x = p.getX();
		float y = p.getY();

		int sectorXOffset = getSectorOffset(x);
		int sectorYOffset = getSectorOffset(y);

		Cell cell = navMesh.getNavCellAt(x, y);
		if (!cell.hasObject()) {

			return getNeighborsFromCell(cell);
		} else {

			this.cellSplit = navMesh.getCellSplit(cell);
			if (this.cellSplit == null) {
				return nList;
			}

			Vector2D sectorP = new Vector2D(sectorXOffset, 192 - sectorYOffset);

			this.cellSplit.triangles()
					.filter((tri) -> tri.contains(sectorP))
					.flatMap(tri -> tri.lines())
					.filter((line) -> line.isNotBlock())
					.forEach((line) -> {
						Node node = new Node();
						node.object = line;
						node.nodeType = NodeType.aTriEdge;
						node.cell = cell;
						nList.add(node);

					});

		}

		nList.forEach((node) -> node.h = getH(node));

		return nList;
	}

	// private Set<Integer> evalLines = new HashSet<>() ;

	private List<Node> getNeighborsFromObjectLine(Node node) {

		List<Node> res = new ArrayList<>();

		ObjectLine line = (ObjectLine) node.object;

		// evalLines.add(line.getId()) ;

		line.getNeighbours().forEach((nl) -> {
			Node newNode = new Node();
			newNode.cell = node.cell;
			newNode.nodeType = NodeType.OBJECT_LINE;
			newNode.object = nl;
			res.add(newNode);

		});

//		
//		ObjectGroundTri tri = line.getNeighbourTriangleA();
//
//		if (tri != null) {
//
//
//			ObjectLine[] lines = tri.getLines();
//			for (ObjectLine l : lines) {
//
//				vp.drawLine(l);
//				Node newNode = new Node();
//				newNode.cell = node.cell;
//				newNode.nodeType = NodeType.OBJECT_LINE;
//				newNode.object = l;
//				res.add(newNode);
//
//			}
//		}
//
//		tri = line.getNeighbourTriangleB();
//
//		if (tri != null) {
//
//
//			ObjectLine[] lines = tri.getLines();
//			for (ObjectLine l : lines) {
//
//				vp.drawLine(l);
//				Node newNode = new Node();
//				newNode.cell = node.cell;
//				newNode.nodeType = NodeType.OBJECT_LINE;
//				newNode.object = l;
//				res.add(newNode);
//				
//			}
//
//		}

		line.getCellObject().entryLines().filter((entryLine) -> {
			Vector2D a = line.getPointA();
			Vector2D b = line.getPointB();
			Vector2D ta = entryLine.getPointA();
			Vector2D tb = entryLine.getPointB();

			boolean r = (Precision.equals(a.x, ta.x, 1f) && Precision.equals(a.y, ta.y, 1f)
					&& Precision.equals(b.x, tb.x, 1f) && Precision.equals(b.y, tb.y, 1f))
					|| (Precision.equals(a.x, tb.x, 1f) && Precision.equals(a.y, tb.y, 1f)
							&& Precision.equals(b.x, ta.x, 1f) && Precision.equals(b.y, ta.y, 1f));

			return r;
		}).forEach((entryLine) -> {

			Vector2D lm = line.getMidPoint();

			lm.x = (-135 + node.getSectorX()) * 192 + ((float) lm.x);
			lm.y = (-92 + node.getSectorY()) * 192 + (192 - ((float) lm.y));
			Cell entryCell = navMesh.getNavCellAt((float) lm.x, (float) lm.y);
			res.addAll(getNeighborsFromCell(entryCell));
//
//			if(entryCell.hasObject()) { 
//				
//				CellSplit cs = navMesh.getCellSplit(entryCell) ; 
//				 
//				if(cs != null) {
//				
//					cs.lines()
//					.filter((l)->{
//						Vector2D a = l.getPointA() ;
//						Vector2D b = l.getPointB() ;
//						Vector2D ta = entryLine.getPointA();
//						Vector2D tb = entryLine.getPointB();
//	
//						return  (Precision.equals(a.x, ta.x, 1f) && Precision.equals(a.y, ta.y, 1f)
//								&& Precision.equals(b.x, tb.x, 1f) && Precision.equals(b.y, tb.y, 1f))
//								|| (Precision.equals(a.x, tb.x, 1f) && Precision.equals(a.y, tb.y, 1f)
//					&&   Precision.equals(b.x, ta.x, 1f) && Precision.equals(b.y, ta.y, 1f));
//	 
//					}).findFirst()
//					.ifPresentOrElse((triEdge)->{
//						 Node n = new Node() ; 
//						 n.cell = entryCell ; 
//						 n.nodeType  = NodeType.aTriEdge ; 
//						 n.object = triEdge ; 
//						 res.add(n) ; 
//					}, ()->{
//						res.addAll(getNeighborsFromCell(entryCell)) ;
//					});
//					
//				}
//			}else { 
//				res.addAll(getNeighborsFromCell(entryCell)) ;
//			}

		});
		;

		return res;
	}

	private CellObject getCellObjectTouch(Cell cell, Line line) {

		return cell.objects().filter((obj) -> {

			return obj.objectLines().anyMatch((outline) -> {
				Vector2D a = outline.getPointA();
				Vector2D b = outline.getPointB();

				Vector2D ta = line.getPointA();
				Vector2D tb = line.getPointB();

				Line2D line1 = new Line2D.Double(a.x, a.y, b.x, b.y);
				Line2D line2 = new Line2D.Double(ta.x, ta.y, tb.x, tb.y);

				return line1.intersectsLine(line2);
				// return (Precision.equals(a.x, ta.x, 1f) && Precision.equals(a.y, ta.y, 1f)
				// || Precision.equals(b.x, tb.x, 1f) && Precision.equals(b.y, tb.y, 1f))
				// || (Precision.equals(a.x, tb.x, 1f) && Precision.equals(a.y, tb.y, 1f)
				// || Precision.equals(b.x, ta.x, 1f) && Precision.equals(b.y, ta.y, 1f));

			});
		}).findFirst().orElse(null);
		// .orElseThrow(()->new IllegalStateException("Could not find target object")) ;

	}

	public static Vector2D getMidPoint(Vector2D pointA, Vector2D pointB) {

		// pointA.x = (-135 + this.cellObject.getCell().getSectorX()) * 192 + ((float)
		// pointA.x);
		// pointA.y = (-92 + this.cellObject.getCell().getSectorY()) * 192 + (192 -
		// ((float) pointA.y));

		// pointB.x = (-135 + this.cellObject.getCell().getSectorX()) * 192 + ((float)
		// pointB.x);
		// pointB.y = (-92 + this.cellObject.getCell().getSectorY()) * 192 + (192 -
		// ((float) pointB.y));
		pointA = pointA.clone();
		pointB = pointB.clone();
		float dx = (float) (pointA.x - pointB.x);
		float dy = (float) (pointA.y - pointB.y);

		pointA.x -= (dx / 2);
		pointA.y -= (dy / 2);

		Vector2D res = new Vector2D(pointA.x, pointA.y);

		res.x = Precision.round(res.x, 3);
		res.y = Precision.round(res.y, 3);
		return res;
	}

	private boolean collinear(int x1, int y1, int x2, int y2, int x3, int y3) {

		/*
		 * Calculation the area of triangle. We have skipped multiplication with 0.5 to
		 * avoid floating point computations
		 */
		int a = x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2);

		return  (a == 0) ; 
	
	}

//	private boolean canPass(CellObject collision, Line line1, Line line2) {
//
//		Vector2D line1Mid = getMidPoint(line1.getPointA(), line1.getPointB());
//		Vector2D line2Mid = getMidPoint(line2.getPointA(), line2.getPointB());
//
//		Line2D.Double moveLine = new Line2D.Double(line1Mid.x, line1Mid.y, line2Mid.x, line2Mid.y);
//
//		return !(collision.objectOutlines().filter((outline) -> outline.isBlockedOutline()).map((outline) -> {
//			Vector2D pointA = outline.getPointA();
//			Vector2D pointB = outline.getPointB();
//
//			return new Line2D.Double(pointA.x, pointA.y, pointB.x, pointB.y);
//		}).anyMatch((line) -> line.intersectsLine(moveLine)));
//
//	}

	
//	private boolean canPass(CellObject coll  , Line target) { 
//		 
//		coll.objectOutlines()
//		.filter((outline)->outline.isBlockedOutline())
//		.anyMatch((outline)->{
//			
//			
//			return false ; 
//		});
//	}
	
	private List<Node> getNeighborsFromCellSplit(Node node) {

		List<Node> res = new ArrayList<>();

		// LineRef line = cs.lineRefs.get(node.index);
		Line line = (Line) node.object;

		CellObject triObject = getCellObjectTouch(node.cell, line);

		line.neighbors().filter((nl) -> nl.isNotBlock()).forEach((nl) -> {

			if (triObject == null) {
				System.out.println("Target object Was Null");
			} else {

//				if (!canPass(triObject, line, nl)) {
	//				System.out.println("Could not pass from " + line + " To " + nl);
	//			}

			}
			Node lNode = new Node();
			lNode.object = nl;
			lNode.nodeType = NodeType.aTriEdge;
			lNode.cell = node.cell;
			res.add(lNode);

		});

		switch (line.getLineType()) {

		case BC:

			Cell cell = node.cell;
			Border targetBorder = cell.borders()
					.filter((border) -> parallelOverlapping(border.getMin(), border.getMax(), line.getPointA(),
							line.getPointB(), false))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Border not exists"));

			final Cell nCell = targetBorder.getNeighbourCell();
			if (!nCell.hasObject()) {

				Node nNode = new Node();
				nNode.object = targetBorder;
				nNode.nodeType = NodeType.CELL_BORDER;
				nNode.cell = node.cell;
				res.add(nNode);

			} else {
				CellSplit nCellSplit = navMesh.getCellSplit(nCell);
				if (nCellSplit == null) {
					return res;
				}

				nCellSplit.lines()
						.filter((l) -> line.getLineType() == LineType.BC)
						.filter((l) -> parallelOverlapping(line.getPointA(), line.getPointB(), l.getPointA(),
								l.getPointB(), true))
						.forEach((l) -> {
							Node nNode = new Node();
							nNode.cell = nCell;
							nNode.nodeType = NodeType.aTriEdge;
							nNode.object = l;
							res.add(nNode);
						});

			}

			break;
		case LC:

			cell = node.cell;

			CellLink targetLink = cell.links()
					.filter((link) -> parallelOverlapping(link.getMin(), link.getMax(), line.getPointA(),
							line.getPointB(), false))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Cell link not exists"));

			Cell neighbourCell = targetLink.getNeighbourCell();

			if (!neighbourCell.hasObject()) {
				Node nNode = new Node();
				nNode.object = targetLink;
				nNode.nodeType = NodeType.CELL_LINK;
				nNode.cell = node.cell;
				res.add(nNode);
			} else {

				CellSplit nCellSplit = navMesh.getCellSplit(neighbourCell);

				if (nCellSplit == null) {
					return res;
				}

				nCellSplit.lines()
						.filter((l) -> l.getLineType() == LineType.LC)
						.filter((l) -> parallelOverlapping(line.getPointA(), line.getPointB(), l.getPointA(),
								l.getPointB(), false))
						.forEach((l) -> {

							Node nNode = new Node();
							nNode.cell = neighbourCell;
							nNode.nodeType = NodeType.aTriEdge;
							nNode.object = l;
							res.add(nNode);
						});

			}

			break;
		case ObjectEntrance:

			node.cell.objects().flatMap((o) -> o.objectInlines()).filter((inline) -> {
				Vector2D a = inline.getPointA();
				Vector2D b = inline.getPointB();

				Vector2D ta = line.getPointA();
				Vector2D tb = line.getPointB();

				boolean r = (Precision.equals(a.x, ta.x, 1f) && Precision.equals(a.y, ta.y, 1f)
						|| Precision.equals(b.x, tb.x, 1f) && Precision.equals(b.y, tb.y, 1f))
						|| (Precision.equals(a.x, tb.x, 1f) && Precision.equals(a.y, tb.y, 1f)
								|| Precision.equals(b.x, ta.x, 1f) && Precision.equals(b.y, ta.y, 1f));

				return r;
			}).findFirst().ifPresent((inline) -> {

				Node n = new Node();
				n.cell = node.cell;
				n.nodeType = NodeType.OBJECT_LINE;
				n.object = inline;
				res.add(n);

			});

			break;
		}

		return res;

	}

	private int getG(Node distNode, Position pos) {

		Float[] dist = getPoints(distNode);

		float dx = dist[0].x - dist[1].x;
		float dy = dist[0].y - dist[1].y;

		Float distPoint = new Float(dist[0].x - dx / 2, dist[0].y - dy / 2);

		return (int) Math.round(distPoint.distance(pos.getX(), pos.getY()));
	}

	private int getG(Node n1, Node n2) {

		Float[] srcLine = getPoints(n1);
		Float[] distLine = getPoints(n2);

		float dx = srcLine[0].x - srcLine[1].x;
		float dy = srcLine[0].y - srcLine[1].y;

		Float src = new Float(srcLine[0].x - dx / 2, srcLine[0].y - dy / 2);

		dx = distLine[0].x - distLine[1].x;
		dy = distLine[0].y - distLine[1].y;

		Float dist = new Float(distLine[0].x - dx / 2, distLine[0].y - dy / 2);

		return (int) Math.round(src.distance(dist));

	}

	private int getH(Node node) {

		Float[] points = getPoints(node);

		float dx = points[0].x - points[1].x;
		float dy = points[0].y - points[1].y;

		points[0].x -= dx / 2;
		points[0].y -= dy / 2;

		double dist = points[0].distance(pStop.getX(), pStop.getY());

		return (int) Math.round((Math.abs(dist) * this.hEstimate));

	}

	Float[] getPoints(Node node) {

//		Segment seg = getSegement(node.sectorYX);
		Float[] points = { new Float(), new Float() };

		switch (node.nodeType) {

		case CELL_BORDER:

			// NavBorder border = seg.getBorderAt(node.index);
			Border border = (Border) node.object;
			Vector2D borderA = border.getMin();
			Vector2D borderB = border.getMax();

			points[0].x = (float) ((-135 + node.getSectorX()) * 192 + borderA.x);
			points[0].y = (float) ((-92 + node.getSectorY()) * 192 + (192 - borderA.y));

			points[1].x = (float) ((-135 + node.getSectorX()) * 192 + borderB.x);
			points[1].y = (float) ((-92 + node.getSectorY()) * 192 + (192 - borderB.y));
			break;
		case CELL_LINK:
			CellLink link = (CellLink) node.object;
			Vector2D linkA = link.getMin();
			Vector2D linkB = link.getMax();

			points[0].x = (float) ((-135 + node.getSectorX()) * 192 + linkA.x);
			points[0].y = (float) ((-92 + node.getSectorY()) * 192 + (192 - linkA.y));

			points[1].x = (float) ((-135 + node.getSectorX()) * 192 + linkB.x);
			points[1].y = (float) ((-92 + node.getSectorY()) * 192 + (192 - linkB.y));

			break;

		case aTriEdge:

			Line line = (Line) node.object;
			Vector2D lineA = line.getPointA();
			Vector2D lineB = line.getPointB();
			short secX = node.getSectorX();

			points[0].x = (-135 + secX) * 192 + ((float) lineA.x);
			points[0].y = (-92 + node.getSectorY()) * 192 + (192 - ((float) lineA.y));

			points[1].x = (-135 + node.getSectorX()) * 192 + ((float) lineB.x);
			points[1].y = (-92 + node.getSectorY()) * 192 + (192 - ((float) lineB.y));

			break;

		case OBJECT_LINE:
			ObjectLine objLine = (ObjectLine) node.object;
			Vector2D pointA = objLine.getPointA();
			Vector2D pointB = objLine.getPointB();
			points[0].x = (-135 + node.getSectorX()) * 192 + ((float) pointA.x);
			points[0].y = (-92 + node.getSectorY()) * 192 + (192 - ((float) pointA.y));

			points[1].x = (-135 + node.getSectorX()) * 192 + ((float) pointB.x);
			points[1].y = (-92 + node.getSectorY()) * 192 + (192 - ((float) pointB.y));

			break;

		}

		return points;
	}

	private List<Node> getNeighborsFromCell(Cell cell) {
		return getNeighborsFromCell(cell, null);
	}

	private List<Node> getNeighborsFromCell(Cell cell, Cell fromCell) {

		// System.out.println("--------------------getNeighborsFromCell(" + sectorYX +
		// "," + cellIndex + "," + fromSegment + "," + fromCell + ")") ;

		List<Node> res = new ArrayList<>();

		res.addAll(extractNeighborsByLinks(cell, fromCell));
		res.addAll(extractNeighborsByBorders(cell, fromCell));
		// res.addAll(extractObjectElements(cell)) ;
//		res.addAll(extractNeighborsByObjects(cell, null)) ; 
		// res.forEach((n)->System.out.println("---------Node { " + n.index + "," +
		// n.cellIndex + "," + n.sectorYX + "," + n.nodeType.name() + "}"));
		return res;

	}

	private Collection<? extends Node> extractNeighborsByBorders(Cell cell, Cell fromCell) {

		List<Node> nList = new ArrayList<>();

		cell.borders().filter((border) -> border.hasNeighbour()).forEach((border) -> {
			Cell neighbour = border.getNeighbourCell();
			if (!neighbour.equals(fromCell)) {

				if (neighbour.hasObject()) {

					// nList.addAll(extractObjectElements(neighbour)) ;
					CellSplit cs = navMesh.getCellSplit(neighbour);

					if (cs != null) {

						cs.lines()
								.filter((l) -> l.getLineType() == LineType.BC)
								.filter((l) -> parallelOverlapping(l.getPointA(), l.getPointB(), border.getMin(),
										border.getMax(), true))
								.forEach((l) -> {

									Node node = new Node();
									node.cell = neighbour;
									node.nodeType = NodeType.aTriEdge;
									node.object = l;
									nList.add(node);
								});

					}
				} else {
					Node node = new Node();
					node.cell = cell;
					node.nodeType = NodeType.CELL_BORDER;
					node.object = border;
					nList.add(node);

				}
			}
		});
		return nList;
	}

	private List<Node> extractNeighborsByLinks(Cell cell, Cell fromCell) {

		List<Node> nList = new ArrayList<>();

		// Segment segment = getSegement(sectorYX);
		// NavCell cell = segment.getCellAt(cellIndex);

		cell.links().filter((link) -> link.hasNeighbour()).forEach((link) -> {
			Cell neighbourCell = link.getNeighbourCell();
			if (!neighbourCell.equals(fromCell)) {

				if (!neighbourCell.hasObject()) {
					Node node = new Node();
					node.cell = link.getCell();
					node.nodeType = NodeType.CELL_LINK;
					node.object = link;

					nList.add(node);

				} else {

					CellSplit cellSplit = navMesh.getCellSplit(neighbourCell);

					// nList.addAll(extractObjectElements(neighbourCell)) ;
					if (cellSplit != null) {

						cellSplit.lines()
								.filter((l) -> l.getLineType() == LineType.LC)
								.filter((l) -> parallelOverlapping(l.getPointA(), l.getPointB(), link.getMin(),
										link.getMax(), false))
								.forEach((l) -> {
									Node node = new Node();
									node.cell = neighbourCell;
									node.nodeType = NodeType.aTriEdge;
									node.object = l;
									nList.add(node);
								});

					}
				}
			}
		});
		return nList;

	}

	private boolean parallelOverlapping(Vector2D p1, Vector2D p2, Vector2D p3, Vector2D p4, boolean bc) {

		if (bc) {
			if (p1.y == p2.y) {

				p1.y = 192 - p1.y;
				p2.y = 192 - p2.y;

			}

			if (p1.x == p2.x) {

				p1.x = 192 - p1.x;
				p2.x = 192 - p2.x;
			}

		}

		boolean sameX = false, sameY = false;

		if (p1.x == p2.x && p3.x == p4.x && p2.x == p3.x) {
			sameX = true;
		} else if (p1.y == p2.y && p3.y == p4.y && p2.y == p3.y) {
			sameY = true;
		}

		if (!(sameX || sameY))
			return false;

		if (sameX) {

			if ((p1.y == p3.y && p2.y == p4.y) || (p2.y == p3.y && p1.y == p4.y)) {
				return true;
			}

			double a = p1.y - p2.y;

			if (valueBetween(p1.y - a, p1.y, p3.y))
				return true;

			if (valueBetween(p1.y - a, p1.y, p4.y))
				return true;

			return false;

		} else {

			if ((p1.x == p3.x && p2.x == p4.x) || (p2.x == p3.x && p1.x == p4.x)) {
				return true;
			}

			double a = p1.x - p2.x;

			if (valueBetween(p1.x - a, p1.x, p3.x))
				return true;

			if (valueBetween(p1.x - a, p1.x, p4.x))
				return true;

			return false;

		}

	}

	boolean valueBetween(double v1, double v2, double vc) {

		return ((v1 > vc && v2 < vc) || (v2 > vc && v1 < vc));
	}

	private void findStart() {

		List<Node> nodes = getNeighborsFromPosition(this.pStart);

		this.oList.clear();

		nodes.forEach((node) -> {
			node.h = getH(node);
			node.g = getG(node, pStart);
			this.oList.put(node.getId(), node);
		});

	}

	public enum NodeType {
		NotSet((byte) 0), CELL_LINK((byte) 1), CELL_BORDER((byte) 2), aTriEdge((byte) 3), OBJECT_LINE((byte) 4);

		private byte value;

		private NodeType(byte val) {
			this.value = val;
		}
	}

	public static int counter = 0;

	public class Node {

		public NodeType nodeType = NodeType.NotSet;
		public Cell cell;
		public Object object;
		// short index;
		public int h;
		public int g;
		public long parent;

		public short getSectorX() {
			return (short) (cell.getSectorYX() & 0xff);
		}

		public short getSectorY() {
			return (short) (cell.getSectorYX() >> 8);
		}

		int getObjectIndex() {
			if (nodeType == NodeType.CELL_BORDER) {
				return ((Border) object).getIndex();
			} else if (nodeType == NodeType.CELL_LINK) {
				return ((CellLink) object).getIndex();
			} else if (nodeType == NodeType.aTriEdge) {
				return ((Line) object).getIndex();
			} else if (nodeType == NodeType.OBJECT_LINE) {
				ObjectLine line = (ObjectLine) object;
				return line.getId();
			} else {
				return -1;
			}
		}

		long getId() {

			return Integer.toUnsignedLong(cell.getIndex()) << 40 | Integer.toUnsignedLong(getObjectIndex()) << 24
					| Integer.toUnsignedLong(nodeType.value) << 16 | cell.getSectorYX();
		}

		@Override
		public String toString() {
			return "Node [nodeType=" + nodeType + ", cellIndex=" + cell.getIndex() + ", sectorYX=" + cell.getSectorYX()
					+ ", h=" + h + ", g=" + g + ", parent=" + parent + "]";
		}

	}

	public static void main(String args[]) {

		String[] arr = { " ", "hello " };

		Stream.of(arr).forEach((s) -> {
			if (s.isBlank())
				return;
			System.out.println(s);
		});

	}

}
