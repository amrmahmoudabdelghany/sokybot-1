package org.sokybot.machine.page.environmentpage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.sokybot.machine.event.DespawnEvent;
import org.sokybot.machine.event.SpawnReachDestinationEvent;
import org.sokybot.machine.event.monsterevent.MonsterDespawnEvent;
import org.sokybot.machine.event.monsterevent.MonsterSpawnEvent;
import org.sokybot.machine.event.trainerevent.TrainerLoadedEvent;
import org.sokybot.machine.gamemodel.IGameModel;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.machinegroup.gamemodel.setting.TrainingArea;
import org.sokybot.machinegroup.gamemodel.setting.TrainingAreaSettings;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.utils.DDSReader;
import org.sokybot.utils.SilkroadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarkLaf;

import lombok.Data;
import net.bytebuddy.asm.Advice.Return;

@Component
public class SroMapViewer extends JPanel {

	private final float scale = 1f;
	private final int sectorWH = (int) (192 * scale);

	private final int rowCount = 3;
	private final int colCount = 3;

	private final int WIDTH = (int) (colCount * sectorWH);
	private final int HEIGHT = (int) (rowCount * sectorWH);

	private final Image defaultImage = new BufferedImage(sectorWH, sectorWH, BufferedImage.TYPE_INT_RGB);

	private final Pair<Integer, Integer>[][] transform = new Pair[][] {
			{ Pair.of(-1, 1), Pair.of(0, 1), Pair.of(1, 1) }, { Pair.of(-1, 0), Pair.of(0, 0), Pair.of(1, 0) },
			{ Pair.of(-1, -1), Pair.of(0, -1), Pair.of(1, -1) } };

	
	@Autowired
	private TrainingAreaSettings areaSettings ; 
	
	// @Autowired
//	private ISroMaterialDAO sroDao;

	@Autowired
	private Trainer trainer;

	private IGameModel gameModel;

	private MinimapLoader minimapLoader = new MinimapLoader();

	private short sectorX = 104;
	private short sectorY = 103;

	private int trainerX = -11078;
	private int trainerY = 2645;

	private EntityLabel trainerLabel = new EntityLabel(Color.YELLOW);

	private Map<Integer, EntityLabel> monsters = new HashMap<>();

	@PostConstruct
	protected void init() {
		setPreferredSize(new Dimension(colCount * sectorWH, rowCount * sectorWH));
		// setLayout(new BorderLayout());
		this.sectorX = SilkroadUtils.getSectorX(this.trainerX);
		this.sectorY = SilkroadUtils.getSectorY(this.trainerY);
		bind();
	}

	private void bind() {
		trainer.addPropertyChangeListener(Trainer.POSITION_PROPERTY, (prop) -> {
			updateTrainerLocation(trainer.getX(), trainer.getY());
		});

	}

	@Override
	public void paint(Graphics g) {

		if (sectorX == -1 || sectorY == -1) {

			g.drawString("Player is not loaded", getWidth() / 2, getHeight() / 2);
		} else {

			int posX = 0;
			int posY = 0;

			for (int x = 0; x < colCount; x++) {
				for (int y = 0; y < rowCount; y++) {

					Image image = getMinimap(y, x);
					g.drawImage(image, posX, posY, null);

					posY += sectorWH;
				}
				posY = 0;
				posX += sectorWH;
			}

			if (this.trainerX != Integer.MAX_VALUE && this.trainerY != Integer.MAX_VALUE) {
//				int sXOffset = (int) (SilkroadUtils.getSectorOffset((float) this.trainerX) * scale);
//				int sYOffset = (int) ((192 - SilkroadUtils.getSectorOffset((float) this.trainerY)) * scale);
//				sXOffset += 192;
//				sYOffset += 192;

//				System.out.println("Actual Grid OffsetX : " + sXOffset ) ; 
//				System.out.println("Actual Grid OffsetY : " + sYOffset) ; 
				
			//	this.trainerLabel.setXOffset(sXOffset);
			//	this.trainerLabel.setYOffset(sYOffset);
			//	this.trainerLabel.paint(g);

				//(int) ((ySector - 92) * 192 + (yOffset / offsetDivisor));
				
				//int originX = SilkroadUtils.getXCoord(0, sector.getLeft().shortValue()) ; 
				
				
//				System.out.println("OriginY Value : " + originY) ; // must 2880
//				System.out.println("TrainerY Value : " + this.trainerY) ; 
				// At this point we  must check if the point to draw is contained into grid rectangle  
				
//				
//				System.out.println("Calc gridOffsetX : " + gridOffsetX) ; 
//				System.out.println("Calc gridOffsetY : " + gridOffsetY) ; 
				Point p = getViewPoint(this.trainerX, this.trainerY); 
				this.trainerLabel.setXOffset(p.x); 
				this.trainerLabel.setYOffset(p.y);
				this.trainerLabel.paint(g);
			}

			// drawing monsters 
			this.monsters.values().forEach((lbl) -> lbl.paint(g));
			
			// drawing training area circle  
			
			TrainingArea area = this.areaSettings.getActiveArea() ; 
			
			Point centerP = getViewPoint(area.getAreaX(), area.getAreaY()) ; 
			int r = area.getAreaR() ; 
			g.setColor(Color.BLUE);
			g.drawOval(centerP.x - r, centerP.y - r, 2 * r, 2 *r);

		}

	}

	private Pair<Integer, Integer> getSector(int x, int y) {
		Pair<Integer, Integer> trans = transform[x][y];
		return Pair.of(this.sectorX + trans.getLeft(), this.sectorY + trans.getRight());
	}

	private Image getMinimap(int gridX, int gridY) {

		// gridY = (colCount - gridY) - 1 ;
		// gridX = (rowCount - gridX) - 1 ;
		Pair<Integer, Integer> targetSec = getSector(gridX, gridY);

		Image image = this.minimapLoader
				.findSectorMinimap(targetSec.getLeft().shortValue(), targetSec.getRight().shortValue())
				.orElseGet(() -> {
					System.out.println("Target Sector : " + targetSec.getLeft() + " , " + targetSec.getRight());
					return this.defaultImage;
				});

		BufferedImage bufferedImage = new BufferedImage((int) (192 * scale), (int) (192 * scale),
				BufferedImage.TYPE_INT_RGB);

		Graphics2D g = bufferedImage.createGraphics();

		g.drawImage(image, 0, 0, (int) sectorWH, (int) sectorWH, null);

		return bufferedImage;
	}

	@EventListener
	public void onTrainerLoaded(TrainerLoadedEvent event) {

		Trainer trainer = event.getTraienr();
		updateTrainerLocation(trainer.getX(), trainer.getY());

	}

	private Point getViewPoint(int x, int y) {

		//  get the position of  the top-left corner of the top-left sector at the grid
		Pair<Integer, Integer> sector = getSector(0, 0);
		int originX = ((sector.getLeft() - 135) * 192) ; 
		int originY = (( sector.getRight() - 92 ) * 192) + 192 ; 
		
		// sub input point from grid origin in order to shift axis
		int gridOffsetX = x - originX  ; 
		int gridOffsetY = ( y - originY ) * -1  ; 
		
		
		return new Point(gridOffsetX, gridOffsetY );
	}

	@EventListener
	public void onMonsterSelected(MonsterSpawnEvent eve) { 
		if(this.monsters.containsKey(eve.getMonster().getUniqueId())) { 
			this.monsters.get(eve.getMonster().getUniqueId()) 
			.setColor(Color.BLUE);
			repaint();
		}
	}
	@EventListener
	public void onSpawnReachDestination(SpawnReachDestinationEvent event) {
		if (this.monsters.containsKey(event.getUniqueId())) {
			EntityLabel m = this.monsters.get(event.getUniqueId());

			Point p = getViewPoint((int) event.getX(), (int) event.getY());
			m.setXOffset(p.x);
			m.setYOffset(p.y);
			repaint();
		}
	}
	
	@EventListener
	public void onDespawn(MonsterDespawnEvent event) { 
		if(this.monsters.containsKey(event.getMonster().getUniqueId())) { 
			this.monsters.remove(event.getMonster().getUniqueId()) ; 
			repaint() ; 
		}
	}

	@EventListener
	public void onMonsterSpawn(MonsterSpawnEvent event) {
		Monster m = event.getMonster();
		EntityLabel lbl = new EntityLabel(Color.RED);

		System.out.println("Monster inside map frame");

		Point p = getViewPoint(m.getX(), m.getY());

		lbl.setXOffset(p.x);
		lbl.setYOffset(p.y);

		monsters.put(m.getUniqueId(), lbl);
		// add(lbl) ;
		repaint();

	}

	private void updateTrainerLocation(int x, int y) {
		this.trainerX = x;
		this.trainerY = y;
		this.sectorX = SilkroadUtils.getSectorX(this.trainerX);
		this.sectorY = SilkroadUtils.getSectorY(this.trainerY);

		repaint();
	}

	public static void main(String args[]) {
		FlatDarkLaf.setup();
		JFrame frame = new JFrame();
		SroMapViewer map = new SroMapViewer();
		map.trainer = new Trainer();
		map.init();
		EnvTab tab = new EnvTab();

		frame.setLayout(new BorderLayout());
		frame.add(map, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(map.WIDTH + 20, map.HEIGHT + 20));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

	}

	private class MinimapLoader {

		private Map<String, Image> tmp = new HashMap<>();
		private String gamePath = "E:\\Amroo\\Silkroad Games\\RedDiamondSRO_RDSRO_1_042\\RedDiamondSRO_RDSRO_1_042";

		public Optional<Image> findSectorMinimap(short x, short y) {
			String id = String.valueOf(x) + "x" + String.valueOf(y);

			Image res = null;
			if (tmp.containsKey(id)) {
				res = tmp.get(id);
			} else {
				Image image = extractMiniMap(x, y);
				tmp.put(id, image);
				res = image;
			}
			return Optional.of(res);
		}

		private Image extractMiniMap(short sectorX, short sectorY) {
			try (IPk2Driver driver = IPk2Driver.open(this.gamePath + "\\Media.pk2")) {
				String name = String.valueOf(sectorX) + "x" + String.valueOf(sectorY) + ".ddj";

				return driver.findFirst("minimap\\" + name).map((jmx) -> {

					try {

						InputStream stream = jmx.getInputStream();
						stream.skip(20);

						byte[] arr = IOUtils.toByteArray(stream);

						// ByteBuffer bytebuffer = ByteBuffer.wrap(IOUtils.toByteArray(stream));

						int[] pixels = DDSReader.read(arr, DDSReader.ARGB, 0);
						int width = DDSReader.getWidth(arr);
						int height = DDSReader.getHeight(arr);
						BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
						image.setRGB(0, 0, width, height, pixels, 0, width);

						return (Image) image;

					} catch (IOException e) {

						e.printStackTrace();
					}
					return null;
				}).orElseGet(() -> {
					System.out.println("Could not load Minimap at " + "minimap\\" + name);
					return (Image) new BufferedImage(192, 1902, BufferedImage.TYPE_4BYTE_ABGR);
				});

			} catch (IOException e1) {
				throw new UncheckedIOException(e1);
			}
		}

	}

	@Data
	private class EntityLabel extends JLabel {

		private int ovalWidth = 10;
		private int ovalHeight = 10;

		private Color color;
		private int xOffset;
		private int yOffset;

		public EntityLabel(Color color) {
			this.color = color;
		}

		@Override
		public void paint(Graphics g) {
			g.setColor(color);

			g.fillOval(xOffset, yOffset, ovalWidth, ovalHeight);
		}

	}

}
