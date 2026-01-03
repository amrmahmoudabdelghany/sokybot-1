package org.sokybot.common;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;
import javax.swing.border.LineBorder;

import org.sokybot.utils.Helper;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarculaLaf;

public class RoundedBorder extends AbstractBorder {

	private final Color color;
	private final int gap;

	public RoundedBorder(Color color, int g) {
		this.color = color;
		this.gap = g;
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		super.paintBorder(c, g, x, y, width, height);
		Graphics2D g2d;
		if (g instanceof Graphics2D) {
			g2d = (Graphics2D) g;
			g2d.setColor(color);
			System.out.println(x + y);
			g2d.draw(new Line2D.Double((double) x, (double) y + 10, (double) x + 3, (double) y + 3));
			g2d.draw(new Line2D.Double((double) x + 3, (double) y + 3, (double) x + 10, (double) y));
			g2d.draw(new Line2D.Double((double) x + 10, (double) y, (double) x + 30, (double) y));
			g2d.draw(new Line2D.Double((double) x + 30, (double) y, (double) x + 33, (double) y + 2));
			g2d.draw(new Line2D.Double((double) x + 33, (double) y + 2, (double) x + 36, (double) y + 8));
			g2d.draw(new Line2D.Double((double) x + 36, (double) y + 8, (double) x + 36, (double) y + 28));
			g2d.draw(new Line2D.Double((double) x + 36, (double) y + 28, (double) x + 34, (double) y + 31));
			g2d.draw(new Line2D.Double((double) x + 34, (double) y + 31, (double) x + 32, (double) y + 33));
			g2d.draw(new Line2D.Double((double) x + 32, (double) y + 33, (double) x + 6, (double) y + 33));
			g2d.draw(new Line2D.Double((double) x + 6, (double) y + 33, (double) x + 3, (double) y + 31));
			g2d.draw(new Line2D.Double((double) x + 3, (double) y + 31, (double) x, (double) y + 27));
			g2d.draw(new Line2D.Double((double) x, (double) y + 27, (double) x, (double) y + 10));
		}
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return (getBorderInsets(c, new Insets(gap, gap, gap, gap)));
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.left = insets.top = insets.right = insets.bottom = gap;
		return insets;
	}

	@Override
	public boolean isBorderOpaque() {
		return true;
	}

	public static Icon getIcon() throws IOException { 
		ResourceLoader loader = new DefaultResourceLoader() ; 
		
		
		return new ImageIcon(loader.getResource("classpath:/icons/char-icon/0x3a29.png").getURL()) ; 
		
	}
	
	public static Image processImage(Image image , int w , int h , int cornerRadius) { 
		
		BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB) ;
		
		Graphics2D g2 = output.createGraphics() ; 
		
		 g2.setComposite(AlphaComposite.Src);
	        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	                RenderingHints.VALUE_ANTIALIAS_ON);
	        g2.setColor(Color.WHITE);
	        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius,
	                cornerRadius));
		
	        g2.setComposite(AlphaComposite.SrcAtop);
	        g2.drawImage(image, 0, 0 , w , h, null);
	        
		g2.dispose(); 
		
		return output ; 
	}
	public static void main(String args[]) throws IOException {

		FlatDarculaLaf.setup();
		
		
		JFrame frame = new JFrame();
		frame.setLayout(new GridBagLayout());

		JPanel panel = new JPanel(new BorderLayout());


		//Icon icon = getIcon() ; 

		
		//JLabel lblCharIcon = new JLabel(new ImageIcon(bimag));
		BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB) ; 
		
		JLabel lblCharIcon = new JLabel(new ImageIcon(processImage(image, 200, 200, 60)));

		panel.add(lblCharIcon, BorderLayout.CENTER);
		frame.add(panel);
		frame.setPreferredSize(new Dimension(800, 800));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

	}

}
