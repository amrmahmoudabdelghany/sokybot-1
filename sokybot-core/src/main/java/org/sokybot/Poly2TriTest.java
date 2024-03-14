package org.sokybot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.commons.math3.util.Precision;
import org.sokybot.machinegroup.gamemodel.geo.Vector2D;


public class Poly2TriTest {

	
	
	public static void main(String args[]) { 
		  
	  
		Vector2D l1pointA = new Vector2D(100 , 50) ;
		Vector2D l1pointB = new Vector2D(100 , 100) ; 
		Vector2D l1Mid = getMidPoint(l1pointA, l1pointB) ; 
		
		
		Vector2D l2pointA = new Vector2D(100 , 200) ;
		Vector2D l2pointB = new Vector2D(100 , 250) ; 
	    Vector2D l2Mid = getMidPoint(l2pointA, l2pointB) ; 
	    		

		
		Polygon p = new Polygon() ; 
		p.addPoint(100, 100); 
		p.addPoint(100, 200); 
		p.addPoint(200, 200);
		p.addPoint(200, 100); 
		
		
		Line2D line = new Line2D.Double() ; 
		line.setLine(l1Mid.x, l1Mid.y, l2Mid.x, l2Mid.y);
		
		System.out.println("Line Intersect : " + intersects(line, p) ) ;
		
		
		JLabel lbl = new JLabel() { 
			
			@Override
			public void paint(Graphics g) {
				g.setColor(Color.BLUE); 
				g.drawLine((int)l1pointA.x, (int)l1pointA.y, (int)l1pointB.x, (int)l1pointB.y);
				g.drawLine((int)l2pointA.x, (int)l2pointA.y, (int)l2pointB.x, (int)l2pointB.y);
				
				g.setColor(Color.GREEN); 
				g.drawOval((int)l1Mid.x, (int)l1Mid.y, 3, 3);
				

				g.setColor(Color.GREEN); 
				g.drawOval((int)l2Mid.x, (int)l2Mid.y, 3, 3);
				
				g.setColor(Color.RED);
				 g.drawPolygon(p);
			}
		}; 
		
		JFrame frame = new JFrame() ; 
		
		frame.setLayout(new BorderLayout()); 
		frame.add(lbl , BorderLayout.CENTER) ; 
		frame.setLocationRelativeTo(null);
		frame.setPreferredSize(new Dimension(400 , 400));
		frame.pack();  
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		
	}
	  public static boolean intersects(Line2D line, Polygon poly) {
	        for (int i = 0; i < poly.npoints; i ++) {
	            int nextI = (i + 1) % poly.npoints;
	            Line2D edge = new Line2D.Double(poly.xpoints[i], poly.ypoints[i], poly.xpoints[nextI], poly.ypoints[nextI]);
	            if (line.intersectsLine(edge)) {
	                return true;
	            }
	        }
	        return false;
	    }

	

	public static Vector2D getMidPoint(Vector2D pointA , Vector2D pointB) { 
 
		
	//	pointA.x = (-135 + this.cellObject.getCell().getSectorX()) * 192 + ((float) pointA.x); 
	//	pointA.y =  (-92 + this.cellObject.getCell().getSectorY()) * 192 + (192 - ((float) pointA.y));
		
		//pointB.x = (-135 + this.cellObject.getCell().getSectorX()) * 192 + ((float) pointB.x); 
	//	pointB.y =  (-92 + this.cellObject.getCell().getSectorY()) * 192 + (192 - ((float) pointB.y));
		pointA = pointA.clone() ; 
		pointB = pointB.clone() ;
		float dx = (float) (pointA.x - pointB.x);
		float dy = (float) (pointA.y - pointB.y);
	   
		pointA.x -= (dx /2) ; 
		pointA.y -= (dy /2) ; 
		
		Vector2D res =  new Vector2D(pointA.x, pointA.y) ;
		
		res.x =  Precision.round(res.x, 3) ;
		res.y =  Precision.round(res.y, 3) ;
		return res ; 
 	}
}
