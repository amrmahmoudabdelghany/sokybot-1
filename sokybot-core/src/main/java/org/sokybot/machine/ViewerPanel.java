package org.sokybot.machine;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.tuple.Pair;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machinegroup.gamemodel.geo.Vector2D;
import org.sokybot.machinegroup.gamemodel.navmesh.Position;

import org.sokybot.machinegroup.gamemodel.navmesh.SectorRef;
import org.sokybot.machinegroup.mapnavigation.Border;
import org.sokybot.machinegroup.mapnavigation.Cell;
import org.sokybot.machinegroup.mapnavigation.CellLink;
import org.sokybot.machinegroup.mapnavigation.CellObject;
import org.sokybot.machinegroup.mapnavigation.NavMesh;
import org.sokybot.machinegroup.mapnavigation.ObjectGroundTri;
import org.sokybot.machinegroup.mapnavigation.ObjectLine;
import org.sokybot.machinegroup.mapnavigation.RuteFinder;
import org.sokybot.machinegroup.mapnavigation.RuteFinder.Node;
import org.sokybot.machinegroup.mapnavigation.RuteFinder.NodeType;
import org.sokybot.machinegroup.mapnavigation.Sector;
import org.sokybot.machinegroup.mapnavigation.triangulation.CellSplit;
import org.sokybot.machinegroup.mapnavigation.triangulation.Line;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.sokybot.utils.SilkroadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import info.clearthought.layout.TableLayout;

@Component
public class ViewerPanel extends JPanel {

	private int rowCount = 3;
	private int colCount = 3;

	private Pair<Integer, Integer>[][] transform = new Pair[][] { { Pair.of(-1, 1), Pair.of(0, 1), Pair.of(1, 1) },
			{ Pair.of(-1, 0), Pair.of(0, 0), Pair.of(1, 0) }, { Pair.of(-1, -1), Pair.of(0, -1), Pair.of(1, -1) } };

	@Autowired
	private ISroMaterialDAO sroDao;

	private JLabel[][] lbls = new JLabel[colCount][rowCount];

	private Map<String, String> sectors = new LinkedHashMap();

	// private short sectorX = 153 ;
	// private short sectorY = 102 ;

	private short sectorX = 104;
	private short sectorY = 103;

	private Point2D startP = new Point(-5790, 2517);
	private Point2D stopP = new Point(-5896, 2552);
	private Point2D currentP = new Point();

	private JCheckBox chkSplit;
	private JCheckBox chkObjOutlines;
	private JCheckBox chkObjInlines;

	private JButton btnLeft;
	private JButton btnRight;
	private JButton btnCenter;
	private JButton btnUp;
	private JButton btnDown;

	private JButton btnfindPath;

	private List<Vector2D> path = new ArrayList<>();

	private NavMesh navMesh;

	private RuteFinder aStar;
	private Graphics pg;

	@Autowired
	private Trainer traienr;

	@PostConstruct
	private void init() {

		this.traienr.addPropertyChangeListener(Trainer.POSITION_PROPERTY, new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				currentP.setLocation(traienr.getX(), traienr.getY());
				repaint();
			}
		});
		this.navMesh = new NavMesh(this.sroDao);

		this.sectorX = SilkroadUtils.getSectorX((float) this.startP.getX());
		this.sectorY = SilkroadUtils.getSectorY((float) this.startP.getY());
		aStar = new RuteFinder(this.sroDao);
		for (int x = 0; x < colCount; x++) {
			for (int y = 0; y < rowCount; y++) {
				final int xx = x;
				final int yy = y;

				lbls[y][x] = new JLabel() {
					@Override
					public void paint(Graphics g) {
						if (pg == null)
							pg = g;

						super.paint(g);
						draw(g, xx, yy);

					}

				};
				// lbls[col][row].setPreferredSize(new Dimension(256 * 2 , 256 * 2));

			}
		}

		this.btnfindPath = new JButton("Find Path !!!");

		this.chkSplit = new JCheckBox("Show Split lines");
		this.chkObjOutlines = new JCheckBox("Show Object Outlines");
		this.chkObjInlines = new JCheckBox("Show Object Inlines");

		ChangeListener cl = new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				repaint();
			}
		};
		this.chkSplit.addChangeListener(cl);
		this.chkObjOutlines.addChangeListener(cl);
		this.chkObjInlines.addChangeListener(cl);

		this.btnUp = new JButton("U");
		this.btnUp.addActionListener((ev) -> {
			sectorY++;
			update();
		});

		this.btnDown = new JButton("D");
		this.btnDown.addActionListener((ev) -> {
			sectorY--;
			update();
		});

		this.btnCenter = new JButton("C");
		this.btnCenter.addActionListener((ev)->update());
		
		this.btnLeft = new JButton("L");
		this.btnLeft.addActionListener((ev) -> {
			sectorX--;
			update();
		});

		this.btnRight = new JButton("R");
		this.btnRight.addActionListener((ev) -> {
			sectorX++;
			update();
		});

		double size[][] = { { TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, 0.25 },
				{ TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED }, };

		TableLayout layout = new TableLayout(size);
		setLayout(layout);

//		for(int col = 0 ; col < colCount ; col++) { 
//			layout.insertColumn(col, TableLayout.FILL);
//			for(int row = 0 ; row < rowCount ; row++) { 
//				layout.insertRow(row, TableLayout.FILL);
//			}
//		}

		// lbls[col][row]

		add(lbls[0][0], "0 , 0"); // 1
		add(lbls[0][1], "0 , 1"); // 2
		add(lbls[0][2], "0 , 2"); // 3
		add(lbls[1][0], "1 , 0"); // 4
		add(lbls[1][1], "1 , 1"); // 5
		add(lbls[1][2], "1 , 2"); // 6
		add(lbls[2][0], "2 , 0"); // 7
		add(lbls[2][1], "2 , 1"); // 8
		add(lbls[2][2], "2 , 2"); // 9
		add(createRightBox(), "3 , 0");

//		lbls[0][0].setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
//		lbls[1][0].setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
//		lbls[2][0].setBorder(BorderFactory.createLineBorder(Color.BLACK, 3)); 
//		lbls[0][0].setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
//		lbls[0][1].setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
//		lbls[1][1].setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
//		lbls[2][1].setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
//		
//		lbls[0][2].setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
//		lbls[1][2].setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
//		lbls[2][2].setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
//	
		bind();
		update();

	}

	private JPanel createBtnBox() {

		int border = 5;
		JPanel panel = new JPanel();
		double[][] size = { { border, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, border },
				{ border, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, border } };
		TableLayout layout = new TableLayout(size);

		panel.setLayout(layout);

		panel.add(btnUp, "2 , 1");
		panel.add(btnLeft, "1 , 2");
		panel.add(btnCenter, "2 , 2");
		panel.add(btnRight, "3 , 2");
		panel.add(btnDown, "2 , 3");

		return panel;

	}

	private static float scale = 1.5f;

	private Box createOptBox() {
		Box box = Box.createVerticalBox();
		box.add(this.chkSplit);
		box.add(this.chkObjOutlines);
		box.add(this.chkObjInlines);
		return box;
	}

	private Box createRightBox() {

		Box box = Box.createVerticalBox();
		JPanel btnBox = createBtnBox();

		box.add(this.btnfindPath);
		box.add(Box.createVerticalStrut(5));
		box.add(btnBox);
		box.add(Box.createVerticalStrut(5));
		box.add(createOptBox());
		box.add(Box.createVerticalGlue());

		return box;

	}

	Random rnd = new Random();

	private int counter = 0;
	public Map<Integer, Color> cellQueue = new HashMap();

	public Set<CellObject> targetObject = new HashSet<>();

	public Set<Integer> lineIds = new HashSet<>();

	public void drawLine(ObjectLine line) {

		targetObject.add(line.getCellObject());

		lineIds.add((line.getId() | line.getCellObject().getRegionId()));

		repaint();
	}

	public void drawCell(Cell cell, Color color) {
		int id = cell.getSectorYX() << 16 | cell.getIndex();

		cellQueue.put(id, color);
		// update();
		repaint();

	}

	public Set<Node> nodes = new HashSet<>();

	public void drawNode(Node node) {
		nodes.add(node);
		repaint();
	}

	private void drawLine(Graphics g, double x1, double y1, double x2, double y2, Color color) {
		g.setColor(color);
		g.drawLine(((int) (x1 * scale)), ((int) (y1 * scale)), ((int) (x2 * scale)), ((int) (y2 * scale)));

	}

	private void draw(Graphics g, int x, int y) {

		Pair<Integer, Integer> sector = getSector(x, y);

		if (this.navMesh.containsSector(sector.getLeft().byteValue(), sector.getRight().byteValue())) {
			Sector sec = this.navMesh.getSector(sector.getLeft().byteValue(), sector.getRight().byteValue());

//			nodes.stream()
//			.parallel()
//					.filter((node) -> node.getSectorX() == sec.getSectorX() && node.getSectorY() == sec.getSectorY())
//					.forEach((node) -> {
//						NodeType nodeType = node.nodeType;
//						if (nodeType == NodeType.CELL_BORDER) {
//							Border b = (Border) node.object;
//							// drawLine(g ,(int) b.getMin().getX() ,(int) b.getMin().getY() ,(int)
//							// b.getMax().getX() ,(int) b.getMax().getY() , Color.GREEN) ;
//
//						} else if (nodeType == NodeType.CELL_LINK) {
//							CellLink b = (CellLink) node.object;
//							// drawLine(g ,(int) b.getMin().getX() ,(int) b.getMin().getY() ,(int)
//							// b.getMax().getX() ,(int) b.getMax().getY() , Color.YELLOW) ;
//
//						} else if (nodeType == NodeType.aTriEdge) {
//							CellSplit cellSplit = navMesh.getCellSplit(node.cell);
//
//							// LineRef line = cellSplit.lineRefs.get(node.index);
//
//							Line line = (Line) node.object;
//							Vector2D lineA = line.getPointA();
//							Vector2D lineB = line.getPointB();
//							 drawLine(g ,(int) lineA.x ,(int) lineA.y ,(int) lineB.x ,(int) lineB.y ,
//							 Color.WHITE) ;
//
//						} else if (nodeType == NodeType.OBJECT_LINE) {
//							ObjectLine line = (ObjectLine) node.object;
//
//							//drawLine(g, (line.getPointA().x), (line.getPointA().y), (line.getPointB().x),
//								//	(line.getPointB().y), (line.isEntryLine()) ? Color.GREEN : Color.RED);
//
//						}
//					});
//
//			this.targetObject.stream()
//			.parallel()
//					.filter((obj) -> obj.getRegionId() == sec.getSectorYX())
//					.flatMap((obj) -> obj.objectLines())
//					.filter((line) -> this.lineIds.contains((line.getId() | line.getCellObject().getRegionId())))
//					.forEach((line) -> {
//
//						//drawLine(g, (line.getPointA().x), (line.getPointA().y), (line.getPointB().x),
//						//		(line.getPointB().y), (line.isEntryLine()) ? Color.GREEN : Color.RED);
//
//					});
			// sec.cells().forEach((cell) -> {
			// if (cell.hasObject()) {
//					g.setColor(new Color(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()));
//					Rectangle2D rect = cell.getRectangle();
//
//					g.drawRect(((int) (rect.getMinX() * scale)), ((int) (rect.getMinY() * scale)),
//							((int) (rect.getWidth() * scale)), ((int) (rect.getHeight() * scale)));
//					CellSplit cellSp = navMesh.getCellSplit(cell);
//					if (cellSp != null) {
//			 cellSp.lineRefs.forEach((line)->{
//				 
//				 Vector2D pointA =     cellSp.points.get(line.a) ;
//				 Vector2D pointB =     cellSp.points.get(line.b) ;
//				
//				// pointA.x *= scale ; 
//				// pointA.y *=scale ; 
//				// pointB.x *= scale ; 
//				// pointB.y *=scale ; 
//				 
//				 if(line.lineType == LineType.NonBlock) { 
//					 g.setColor(Color.WHITE);
//				 }else if(line.lineType == LineType.Block) { 
//					 g.setColor(Color.RED) ;
//				 }else if(line.lineType == LineType.BC) { 
//					 g.setColor(Color.GREEN) ;
//				 }else if(line.lineType == LineType.LC) { 
//					 g.setColor(Color.CYAN) ; 
//				 }else if(line.lineType == LineType.ObjectEntrance) { 
//					 g.setColor(Color.YELLOW) ;
//				 }
//				 
//				 g.drawLine((int)pointA.x,(int) pointA.y, (int)pointB.x, (int)pointB.y) ; 
//				 
//			 });
			// }
			// }
			// });
			if(sec != null) {
			sec.cells().parallel().forEach((cell) -> {
				int id = cell.getSectorYX() << 16 | cell.getIndex();
				if (cellQueue.containsKey(id)) {

					Rectangle2D rect = cell.getRectangle();
					g.setColor(cellQueue.get(id));
					g.drawRect(((int) (rect.getMinX() * scale)), ((int) (rect.getMinY() * scale)),
							((int) (rect.getWidth() * scale)), ((int) (rect.getHeight() * scale)));

				} else {
					Rectangle2D rect = cell.getRectangle();
					g.setColor(Color.GREEN);
					g.drawRect(((int) (rect.getMinX() * scale)), ((int) (rect.getMinY() * scale)),
							((int) (rect.getWidth() * scale)), ((int) (rect.getHeight() * scale)));

				}
				if (cell.hasObject()) {
					
					
					if (this.chkObjOutlines.isSelected()) {
						cell.objects().flatMap((obj) -> obj.objectOutlines()).forEach((outline) -> {

							if (outline.getLineFlag() == 0) {
								g.setColor(Color.GREEN);
							} else if (outline.getLineFlag() == 3) {
								g.setColor(Color.RED);
							} else if (outline.getLineFlag() == 131) {
								g.setColor(Color.pink);
							} else if (outline.getLineFlag() == 16) {
								g.setColor(Color.CYAN);
							} else if (outline.getLineFlag() == 8) {
								g.setColor(Color.CYAN);
							} else {
								g.setColor(Color.BLACK);
							}
							Vector2D pointA = outline.getPointA();
							Vector2D pointB = outline.getPointB();

							g.drawLine(((int) (pointA.x * scale)), ((int) (pointA.y * scale)),
									((int) (pointB.x * scale)), ((int) (pointB.y * scale)));

						});
					}

					if (this.chkObjInlines.isSelected()) {
						cell.objects().flatMap((obj) -> obj.objectInlines()).forEach((inline) -> {

							if (inline.getLineFlag() == 4) {
								g.setColor(Color.YELLOW);
							} else if (inline.getLineFlag() == 7) {
								g.setColor(Color.RED);
							} else if (inline.getLineFlag() == 135) {
								g.setColor(Color.pink);
							} else if (inline.getLineFlag() == 20) {
								g.setColor(Color.CYAN);
							} else {
								g.setColor(Color.BLACK);
							}
							
							
							
							Vector2D pointA = inline.getPointA();
							Vector2D pointB = inline.getPointB();

							g.drawLine(((int) (pointA.x * scale)), ((int) (pointA.y * scale)),
									((int) (pointB.x * scale)), ((int) (pointB.y * scale)));

						});

						if (this.chkSplit.isSelected()) {

							CellSplit cs = this.navMesh.getCellSplit(cell);
							if (cs != null) {
								cs.lines().forEach((line) -> {
									switch (line.getLineType()) {
									case Block:
										g.setColor(Color.RED); // may not valid
										break;
									case LC:
										g.setColor(Color.darkGray);
										break;
									case BC:
										g.setColor(Color.BLUE);
										break;
									case NonBlock:
										g.setColor(Color.WHITE);
										break;
									case ObjectEntrance:
										g.setColor(Color.ORANGE);
										break;
									default:
										g.setColor(Color.black);
										break;

									}
									Vector2D pointA = line.getPointA();
									Vector2D pointB = line.getPointB();

									g.drawLine(((int) (pointA.x * scale)), ((int) (pointA.y * scale)),
											((int) (pointB.x * scale)), ((int) (pointB.y * scale)));

								});

							}
						}
					}
				}
//					cell.objects()
//					.flatMap((obj)->obj.objectLines())
//					.forEach((line)->{
//						if(line.isEntryLine()) { 
//							Vector2D pointA = line.getPointA() ; 
//							Vector2D pointB = line.getPointB() ; 
//							g.setColor(Color.GREEN) ;
//							g.drawLine(((int) (pointA.x * scale)), ((int) (pointA.y * scale)),
//									((int) (pointB.x * scale)), ((int) (pointB.y * scale)));
//	
//							Vector2D mid = line.getMidPoint() ; 
//							g.setColor(Color.BLACK) ; 
//							
//							
//								
//						}
//					});
//					CellSplit cs = this.navMesh.getCellSplit(cell) ; 
//					cs.lines()
//					.forEach((line)->{
//						
//					});
//					
				

				// Rectangle2D rect = cell.getRectangle();
				// g.setColor(Color.GREEN);
				// g.drawRect(((int) (rect.getMinX() * scale)), ((int) (rect.getMinY() *
				// scale)),
				// ((int) (rect.getWidth() * scale)), ((int) (rect.getHeight() * scale)));
				// cell.objects().flatMap(CellObject::objectGroundTriangles).forEach((tri) -> {
				// Polygon p = new Polygon();
				// p.addPoint((int) (tri.getPointA().x * scale), (int) (tri.getPointA().y *
				// scale));
				// p.addPoint((int) (tri.getPointB().x * scale), (int) (tri.getPointB().y *
				// scale));
				// p.addPoint((int) (tri.getPointC().x * scale), (int) (tri.getPointC().y *
				// scale));
				// g.setColor(Color.MAGENTA);
				// g.drawPolygon(p);
				// });

//				cell.objects()
//				.parallel()
//						.filter((object) -> object.hasEntrance())
//
//						.flatMap(CellObject::objectInlines)
//						// .filter((outline) -> outline.getLineFlag() == 0)
//						.forEach((outline) -> {
//							if (outline.hasNeighbour()) {
//								// System.out.println("ViewerPanael::draw --> Outline object has neighbour
//								// triangle(s)");
//
//								ObjectGroundTri tri = outline.getNeighbourTriangleA();
//
//								if (tri != null) {
//
//									Vector2D pa = tri.getPointA();
//									Vector2D pb = tri.getPointB();
//									Vector2D pc = tri.getPointC();
//
//									pa.x *= scale;
//									pa.y *= scale;
//									pb.x *= scale;
//									pb.y *= scale;
//									pc.x *= scale;
//									pc.y *= scale;
//
//									Polygon p = new Polygon();
//
////									p.addPoint((int) pa.x, (int) pa.y);
////									p.addPoint((int) pb.x, (int) pb.y);
////									p.addPoint((int) pc.x, (int) pc.y);
////									g.setColor(Color.YELLOW);
////									g.drawLine((int)pa.x,(int) pa.y,(int) pb.x,(int) pb.y);
////									g.drawLine((int)pb.x,(int) pb.y,(int) pc.x,(int) pc.y);
////									g.drawLine((int)pc.x,(int) pc.y,(int) pa.x,(int) pa.y);
////									
////									counter++;
//
//								}
//
//								tri = outline.getNeighbourTriangleB();
//
//								if (tri != null) {
//
//									Vector2D pa = tri.getPointA();
//									Vector2D pb = tri.getPointB();
//									Vector2D pc = tri.getPointC();
//
//									pa.x *= scale;
//									pa.y *= scale;
//									pb.x *= scale;
//									pb.y *= scale;
//									pc.x *= scale;
//									pc.y *= scale;
//
//									Polygon p = new Polygon();
//
//									p.addPoint((int) pa.x, (int) pa.y);
//									p.addPoint((int) pb.x, (int) pb.y);
//									p.addPoint((int) pc.x, (int) pc.y);
//									g.setColor(Color.CYAN);
//									// g.drawPolygon(p) ;
//								}
//
//								Vector2D pointA = outline.getPointA();
//								Vector2D pointB = outline.getPointB();
//								// System.out.println("Outline flag " + outline.getLineFlag());
//								if (outline.getLineFlag() == 0 || outline.getLineFlag() == 4) {
//									g.setColor(Color.RED);
//									// g.drawLine(((int) (pointA.x * scale)), ((int) (pointA.y * scale)),
//									// ((int) (pointB.x * scale)), ((int) (pointB.y * scale)));
//								} else {
//									// System.out.println("Line Flag : " + outline.getLineFlag());
//									g.setColor(Color.BLACK);
//									// g.drawLine(((int) (pointA.x * scale)), ((int) (pointA.y * scale)),
//									// ((int) (pointB.x * scale)), ((int) (pointB.y * scale)));
//
//								}
//
//							}
//
//						});
			});
			}

		}
		if (this.startP != null) {

			short startPSecX = SilkroadUtils.getSectorX((float) this.startP.getX());
			short startPSecY = SilkroadUtils.getSectorY((float) this.startP.getY());
			if (startPSecX == sector.getLeft() && startPSecY == sector.getRight()) {
				int sXOffset = (int) (SilkroadUtils.getSectorOffset((float) this.startP.getX()) * scale);
				int sYOffset = (int) ((192 - SilkroadUtils.getSectorOffset((float) this.startP.getY())) * scale);
				// g.drawOval(sXOffset, sYOffset, 10 ,10);
				g.setColor(Color.GREEN);
				g.fillOval(sXOffset, sYOffset, 10, 10);
				System.out.println("Drawing at " + sXOffset + " , " + sYOffset);
				// Cell startCell = this.navMesh.getNavCellAt((float)this.startP.getX(),(float)
				// this.startP.getY()) ;

				// drawCellNeighbors(g, startCell);

			}

		}

		if (this.stopP != null) {

			short stopPSecX = SilkroadUtils.getSectorX((float) this.stopP.getX());
			short stopPSecY = SilkroadUtils.getSectorY((float) this.stopP.getY());

			if (stopPSecX == sector.getLeft() && stopPSecY == sector.getRight()) {
				int sXOffset = (int) (SilkroadUtils.getSectorOffset((float) this.stopP.getX()) * scale);
				int sYOffset = (int) ((192 - SilkroadUtils.getSectorOffset((float) this.stopP.getY())) * scale);
				// g.drawOval(sXOffset, sYOffset, 10 ,10);
				g.setColor(Color.red);
				g.fillOval(sXOffset, sYOffset, 10, 10);

			}

		}

		if (this.currentP != null) {

			short stopPSecX = SilkroadUtils.getSectorX((float) this.currentP.getX());
			short stopPSecY = SilkroadUtils.getSectorY((float) this.currentP.getY());

			if (stopPSecX == sector.getLeft() && stopPSecY == sector.getRight()) {
				int sXOffset = (int) (SilkroadUtils.getSectorOffset((float) this.currentP.getX()) * scale);
				int sYOffset = (int) ((192 - SilkroadUtils.getSectorOffset((float) this.currentP.getY())) * scale);
				// g.drawOval(sXOffset, sYOffset, 10 ,10);
				g.setColor(Color.YELLOW);
				g.fillOval(sXOffset, sYOffset, 10, 10);

			}

		}

		if (path != null && !path.isEmpty()) {
			for (int i = 0; i < path.size() - 1; i++) {
				Vector2D p1 = path.get(i);
				Vector2D p2 = path.get(i + 1);

				int x1 = (int) (((sector.getLeft() - 135) * 192 - p1.x) * -scale);
				int y1 = (int) (((sector.getRight() - 92) * 192 - p1.y) * scale);
				int x2 = (int) (((sector.getLeft() - 135) * 192 - p2.x) * -scale);
				int y2 = (int) (((sector.getRight() - 92) * 192 - p2.y) * scale);

				g.setColor(Color.yellow);
				g.fillOval(x1, y1 + 288, 5, 5);
				g.fillOval(x2, y2 + 288, 5, 5);
				g.setColor(Color.blue);
				g.drawLine(x1, 288 + y1, x2, 288 + y2);

			}
		}

	}

	private void drawCellNeighbors(Graphics g, Cell cell) {

		List<CellLink> links = cell.links().collect(Collectors.toList());
		if (links.isEmpty())
			drawRect(g, cell.getRectangle(), Color.CYAN);
		else {
			drawRect(g, cell.getRectangle(), Color.GREEN);
		}
		for (CellLink b : links) {
			Vector2D a = b.getMin();
			Vector2D c = b.getMax();
			g.setColor(Color.BLUE);

			g.drawLine((int) (a.x * scale), (int) (a.y * scale), (int) (c.x * scale), (int) (c.y * scale));

			if (b.hasNeighbour())
				drawRect(g, b.getNeighbourCell().getRectangle(), Color.YELLOW);
		}

	}

	private void drawRect(Graphics g, Rectangle2D rect, Color color) {
		g.setColor(color);
		g.drawRect(((int) (rect.getMinX() * scale)), ((int) (rect.getMinY() * scale)),
				((int) (rect.getWidth() * scale)), ((int) (rect.getHeight() * scale)));

	}

	private void bind() {

		this.btnfindPath.addActionListener((ev) -> {
			if (this.startP != null && this.stopP != null) {
				path = aStar.findPath((float) startP.getX(), (float) startP.getY(), (float) stopP.getX(),
						(float) stopP.getY(), 1);

				// path = aStar.findPath(new Position((float)startP.getX(), 0f,
				// (float)startP.getY()),
				// new Position((float)stopP.getX() , 0f , (float)stopP.getY()), 1) ;
				System.out.println("Start : " + this.startP);
				System.out.println("Stop : " + this.stopP);
				repaint();
			}
		});
		for (int x = 0; x < colCount; x++) {
			for (int y = 0; y < rowCount; y++) {
				lbls[y][x].addMouseListener(createMouseAdapter(x, y));
			}
		}
	}

	private MouseAdapter createMouseAdapter(final int x, final int y) {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

				int btn = e.getButton();

				if (btn == MouseEvent.BUTTON1 || btn == MouseEvent.BUTTON3) {
					Pair<Integer, Integer> sector = getSector(x, y);
					Point point = new Point();

					point.x = (int) ((-135 + sector.getLeft()) * 192 + ((e.getX() / scale)));
					point.y = (int) ((-92 + sector.getRight()) * 192 + (288 - e.getY()) / scale);

					if (btn == MouseEvent.BUTTON1) {
						startP = point;
						System.out.println("Start : [" + startP.getX() + " , " + startP.getY() + "]");
					} else if (btn == MouseEvent.BUTTON3) {
						stopP = point;

					}

					repaint();

				}
			}
		};
	}

	private Pair<Integer, Integer> getSector(int x, int y) {
		Pair<Integer, Integer> trans = transform[x][y];
		return Pair.of(this.sectorX + trans.getLeft(), this.sectorY + trans.getRight());
	}

	private void update() {

		for (int x = 0; x < rowCount; x++) {
			for (int y = 0; y < colCount; y++) {
				Pair<Integer, Integer> sector = getSector(x, y);
				lbls[y][x].setIcon(new ImageIcon(
						getMiniMap(sector.getLeft().shortValue(), sector.getRight().shortValue(), scale)));
			}
		}

	}

	private Image getMiniMap(short sectorX, short sectorY, float scale) {
		BufferedImage bufferedImage = new BufferedImage((int) (192 * scale), (int) (192 * scale),
				BufferedImage.TYPE_INT_RGB);

		Image image = sroDao.findSectorMinimap(sectorX, sectorY).get();

		Graphics2D g = bufferedImage.createGraphics();

		g.drawImage(image, 0, 0, (int) (192 * scale), (int) (192 * scale), null);
		return bufferedImage;
	}

}
