package org.sokybot.machinegroup.gamemodel.navmesh;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Entity
@Getter
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
