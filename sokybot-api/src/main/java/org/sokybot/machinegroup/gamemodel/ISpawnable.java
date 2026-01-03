package org.sokybot.machinegroup.gamemodel;

import java.awt.Point;
import java.awt.geom.Point2D;

import org.sokybot.utils.SilkroadUtils;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ISpawnable extends SilkroadEntity {

	private int uniqueId;

	private short xSector;
	private short ySector;

	private float xOffset;
	private float yOffset;
	private float zOffset;
	private short angle;

	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	protected Point position = new Point(0, 0);

	

	// public Position getPosition() {

	// return (position == null) ?
	// new Position(SilkroadUtils.getXCoord(xOffset, xSector),
	// SilkroadUtils.getYCoord(yOffset, ySector)) :
	// this.position ;
	// }

	public int getX() {
		return this.position.x;
	}

	public int getY() {
		return this.position.y;
	}

	public void setLocation(int x, int y) {
		this.position.setLocation(x, y);
	}

	public void translate(int x , int y ) { 
		this.position.translate(x, y);
	}
	public double distance(double px, double py) {
		return this.position.distance(new Point2D.Double(px, py));
	}

	public boolean isInCave() {
		return (this.xSector << 8 | this.ySector) >= (((Short.MAX_VALUE << 1) + 1) & 0xFFFF);
	}

}
