package org.sokybot.machine.event.trainerevent;

import java.awt.Point;

import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.utils.SilkroadUtils;
import org.springframework.context.ApplicationEvent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = false)
public class TrainerStuckEvent extends ApplicationEvent {

	private byte xSector;
	private byte ySector;
	private float x;
	private float y;
	private float z;
	private short angle;

	public TrainerStuckEvent(Object source, ImmutablePacket packet) {
		super(source);
		IStreamReader reader = packet.getStreamReader();
		reader.getInt(); // unique id
		this.xSector = reader.getByte();
		this.ySector = reader.getByte();
		this.x = reader.getFloat();
		this.z = reader.getFloat();
		this.y = reader.getFloat();
		this.angle = SilkroadUtils.getAngle(reader.getShort());

	}
	
	public Point getLocation() { 
		return new Point(SilkroadUtils.getXCoord(this.x, this.xSector), SilkroadUtils.getYCoord(y, ySector)) ;
	}

}
