package org.sokybot.machineui.page.environment;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.machineui.model.MachineViewModel;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.utils.DDSReader;
import org.sokybot.utils.SilkroadUtils;

import lombok.Data;

public class SroMapViewer extends JPanel {

    private static final long serialVersionUID = 1L;
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

    private final MachineViewModel viewModel;
    private final MinimapLoader minimapLoader = new MinimapLoader();

    private short sectorX = 104;
    private short sectorY = 103;

    private int trainerX = -11078;
    private int trainerY = 2645;

    private EntityLabel trainerLabel = new EntityLabel(Color.YELLOW);
    private Map<Integer, EntityLabel> monsters = new HashMap<>();

    public SroMapViewer(MachineViewModel viewModel) {
        this.viewModel = viewModel;
        init();
    }

    protected void init() {
        setPreferredSize(new Dimension(colCount * sectorWH, rowCount * sectorWH));
        bind();
    }

    private void bind() {
        // Listen to model updates
        this.viewModel.addTrainerListener(() -> {
            Trainer t = viewModel.getTrainer();
            if (t != null) {
                updateTrainerLocation(t.getX(), t.getY());
            }
        });
        
        this.viewModel.addMonsterListener(() -> {
             refreshMonsters();
             repaint();
        });
    }
    
    private void refreshMonsters() {
        // Sync labels with model monsters
        Map<Integer, Monster> currentMonsters = viewModel.getMonsters();
        
        // Remove dead
        monsters.keySet().removeIf(id -> !currentMonsters.containsKey(id));
        
        // Add/Update
        currentMonsters.values().forEach(m -> {
             EntityLabel lbl = monsters.computeIfAbsent(m.getUniqueId(), k -> new EntityLabel(Color.RED));
             Point p = getViewPoint(m.getX(), m.getY());
             lbl.setXOffset(p.x);
             lbl.setYOffset(p.y);
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
                Point p = getViewPoint(this.trainerX, this.trainerY);
                this.trainerLabel.setXOffset(p.x);
                this.trainerLabel.setYOffset(p.y);
                this.trainerLabel.paint(g);
            }

            this.monsters.values().forEach((lbl) -> lbl.paint(g));
            
            // Training area circle? Requires settings service.
            // Skipping for now or TODO
        }
    }

    private Pair<Integer, Integer> getSector(int x, int y) {
        Pair<Integer, Integer> trans = transform[x][y];
        return Pair.of(this.sectorX + trans.getLeft(), this.sectorY + trans.getRight());
    }

    private Image getMinimap(int gridX, int gridY) {
        Pair<Integer, Integer> targetSec = getSector(gridX, gridY);
        return this.minimapLoader
                .findSectorMinimap(targetSec.getLeft().shortValue(), targetSec.getRight().shortValue())
                .orElse(this.defaultImage);
    }
    
    private void updateTrainerLocation(int x, int y) {
        this.trainerX = x;
        this.trainerY = y;
        this.sectorX = SilkroadUtils.getSectorX(this.trainerX);
        this.sectorY = SilkroadUtils.getSectorY(this.trainerY);
        SwingUtilities.invokeLater(this::repaint);
    }

    private Point getViewPoint(int x, int y) {
        Pair<Integer, Integer> sector = getSector(0, 0);
        int originX = ((sector.getLeft() - 135) * 192);
        int originY = ((sector.getRight() - 92) * 192) + 192;
        int gridOffsetX = x - originX;
        int gridOffsetY = (y - originY) * -1;
        return new Point(gridOffsetX, gridOffsetY);
    }

    private class MinimapLoader {
        private Map<String, Image> tmp = new HashMap<>();
        // Hardcoded path from original? Should be config.
        private String gamePath = "E:\\Amroo\\Silkroad Games\\RedDiamondSRO_RDSRO_1_042\\RedDiamondSRO_RDSRO_1_042";

        public Optional<Image> findSectorMinimap(short x, short y) {
            String id = x + "x" + y;
            if (tmp.containsKey(id)) return Optional.of(tmp.get(id));

            try (IPk2Driver driver = IPk2Driver.open(this.gamePath + "\\Media.pk2")) {
                String name = x + "x" + y + ".ddj";
                Optional<Image> img = driver.findFirst("minimap\\" + name).map((jmx) -> {
                    try {
                        InputStream stream = jmx.getInputStream();
                        stream.skip(20);
                        byte[] arr = IOUtils.toByteArray(stream);
                        int[] pixels = DDSReader.read(arr, DDSReader.ARGB, 0);
                        int width = DDSReader.getWidth(arr);
                        int height = DDSReader.getHeight(arr);
                        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                        image.setRGB(0, 0, width, height, pixels, 0, width);
                        return image;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                });
                img.ifPresent(i -> tmp.put(id, i));
                return img;
            } catch (IOException e1) {
                // throw new UncheckedIOException(e1);
                return Optional.empty();
            }
        }
    }

    @Data
    private class EntityLabel extends JLabel {
        private static final long serialVersionUID = 1L;
        private int xOffset;
        private int yOffset;
        private Color color;

        public EntityLabel(Color color) {
            setForeground(color);
            setSize(10, 10);
            this.color = color;
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(color);
            g.fillOval(xOffset, yOffset, 10, 10);
        }
    }
}
