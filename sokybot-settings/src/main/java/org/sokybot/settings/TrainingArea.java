package org.sokybot.settings;

import java.awt.Point;
import java.io.Serializable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
public class TrainingArea implements Serializable {

	private static final long serialVersionUID = 1L;

	@Setter(value = AccessLevel.NONE)
	private Integer id = null;

	@Setter(value = AccessLevel.NONE)
	private TrainingAreaSettings areaSettings;

	private String name;

	private int areaX = 0;
	private int areaY = 0;
	private int areaR = 0;

	public Integer getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getAreaX() {
		return areaX;
	}
	
	public int getAreaY() {
		return areaY;
	}
	
	public int getAreaR() {
		return areaR;
	}
	
	public TrainingAreaSettings getAreaSettings() {
		return areaSettings;
	}
	
	public TrainingArea(TrainingAreaSettings areaSettings, String name, int areaX, int areaY, int areaR) {
		super();
		this.areaSettings = areaSettings;
		this.name = name;
		this.areaX = areaX;
		this.areaY = areaY;
		this.areaR = areaR;
	}

	public Point getRndPoint() {
		Point point = new Point();
		int angle = (int) (Math.random() * 360);

		point.x = (int) (areaX + (areaR * Math.sin(angle)));
		point.y = (int) (areaY + (areaR * Math.cos(angle)));

		return point;
	}

	public boolean contains(int x, int y) {
		return (distance(x , y ) <= (areaR)) ; 
	}

	public double distance(int px, int py) {
		px -= areaX;
		py -= areaY;
		return Math.sqrt(px * px + py * py);
	}

}
