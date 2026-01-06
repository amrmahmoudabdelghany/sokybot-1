package org.sokybot.persistence.entities.navmesh;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Entity
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ObjectLineRef implements Serializable {

	
	//public static final byte IN_LINE = 0 ; 
	//public static final byte OUT_LINE = 1 ; 
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id ; 
	
	private short pointAIndex;
	private short pointBIndex;

	private int neighbourAIndex; // index of neighbour triangle A --> ObjectGround
	private int neighbourBIndex; // index of neighbour triangle B --> ObjectGround --> FFFF --> no Neighbour triangle

	private byte flag;
	
	public int getId() { return id; }
	public short getPointAIndex() { return pointAIndex; }
	public short getPointBIndex() { return pointBIndex; }
	public int getNeighbourAIndex() { return neighbourAIndex; }
	public int getNeighbourBIndex() { return neighbourBIndex; }
	public byte getFlag() { return flag; }
	
	public void setId(int id) { this.id = id; }
	public void setPointAIndex(short pointAIndex) { this.pointAIndex = pointAIndex; }
	public void setPointBIndex(short pointBIndex) { this.pointBIndex = pointBIndex; }
	public void setNeighbourAIndex(int neighbourAIndex) { this.neighbourAIndex = neighbourAIndex; }
	public void setNeighbourBIndex(int neighbourBIndex) { this.neighbourBIndex = neighbourBIndex; }
	public void setFlag(byte flag) { this.flag = flag; }
	
	

	public ObjectLineRef(short pointAIndex,
			short pointBIndex,
			short neighbourAIndex,
			short neighbourBIndex, 
			byte flag  ) {
		super();
		this.pointAIndex = pointAIndex;
		this.pointBIndex = pointBIndex;
		this.neighbourAIndex = neighbourAIndex & 0xffff ;
		this.neighbourBIndex = neighbourBIndex & 0xffff ;
		this.flag = flag;
	
	}
	
	

	@Override
	public boolean equals(Object obj) {
		
		if(!(obj instanceof ObjectLineRef)) return false ; 
		
		ObjectLineRef other = (ObjectLineRef) obj ; 
		return  flag == other.flag && id == other.id && neighbourAIndex == other.neighbourAIndex
					&& neighbourBIndex == other.neighbourBIndex && pointAIndex == other.pointAIndex
					&& pointBIndex == other.pointBIndex;
		
	}
	
	public boolean hasNeighbour() { 
		return (neighbourAIndex != 65535 || neighbourBIndex != 65535) ; 
	}
	
	

}
