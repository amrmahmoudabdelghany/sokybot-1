package org.sokybot.persistence.entities.navmesh;

import java.awt.geom.Point2D.Float;
import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.sokybot.persistence.entities.geo.Vector2D;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Position implements Serializable {

	
	@Id
	@GeneratedValue(strategy =  GenerationType.AUTO)
	private int id ; 
	
	private float x ; 
	private float z ;
	private float y ; 
	
	private byte flag ; 
	
	
	
	
	
	public byte getSectorY() { 
		return(byte) Math.floor(y/192 + 92) ; 
	}
	
	public byte getSectorX() { 
		return (byte) Math.floor(x / 192 + 135) ; 
	}
	
	public int getXOffset() {
		int res = (int) (x % 192) ; 
		 
		if(res < 0) { 
			res += 192 ; 
		}
		
		return res ; 
	}
	
	
	public int getYOffset() { 
		int res = (int) (y % 192) ; 
		
		if(res < 0) { 
			res += 192 ; 
		}
		
		return res ;
		
	}

	public Position(float x, float z, float y) {
		super();
		this.x = x;
		this.z = z;
		this.y = y;
	}
	
	
	public Vector2D toVector2D() { 
		return new Vector2D(this.x  , this.y) ;
	}
	public Float toFloat() { 
		return new Float(this.x, this.y) ; 
	}
	
	
	
}
