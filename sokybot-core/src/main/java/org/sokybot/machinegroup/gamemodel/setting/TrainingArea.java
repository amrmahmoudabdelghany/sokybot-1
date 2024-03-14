package org.sokybot.machinegroup.gamemodel.setting;

import java.awt.Point;
import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class TrainingArea implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Setter(value = AccessLevel.NONE)
	private Integer id = null;

	@ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.MERGE, CascadeType.REMOVE })
	@JoinColumn(name = "area_setting_id")
	@Setter(value = AccessLevel.NONE)
	@Getter(value = AccessLevel.NONE)
	private TrainingAreaSettings areaSettings;

	@Column(name = "name")
	private String name;

	private int areaX = 0;
	private int areaY = 0;
	private int areaR = 0;

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
