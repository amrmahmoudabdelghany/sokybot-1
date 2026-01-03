package org.sokybot.machinegroup.gamemodel.navmesh;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;

import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.sokybot.machinegroup.mapnavigation.triangulation.Point;

import lombok.Getter;
import lombok.ToString;


//https://github.com/DummkopfOfHachtenduden/SilkroadDoc/wiki/JMXVBMS
@Entity
@ToString
public class ObjectNavMesh implements Serializable {

	
	@Id
	//@GeneratedValue(strategy = GenerationType.AUTO)
	private int id ; 
	
	
	@OneToMany(cascade = CascadeType.ALL , fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private  List<Position> points = new ArrayList<>() ; 



	@OneToMany(cascade = CascadeType.ALL , fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@JoinColumn(name = "nav_inline")
	private  List<ObjectLineRef> inLines =  new ArrayList<>() ;


	@OneToMany(cascade = CascadeType.ALL , fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@JoinColumn(name = "nav_outline")
	private  List<ObjectLineRef> outLines =  new ArrayList<>(); 


	@OneToMany(cascade = CascadeType.ALL , fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private  List<ObjectGroundTriRef> objectGround =  new ArrayList<>() ; 

	@Getter
	private boolean outCanBlock , inCanBlock , hasEntrance  ; 
	
	

	

	public Position getPointAt(int index) { 
		return this.points.get(index) ; 
	}
	
	public List<Position> points() { 
		return Collections.unmodifiableList(this.points) ; 
	}
	
	public List<ObjectLineRef> outLines() { 
		return Collections.unmodifiableList(this.outLines) ; 
	}
	
	public List<ObjectLineRef> inLines() { 
		return Collections.unmodifiableList(this.inLines) ; 
	}
	
	public List<ObjectGroundTriRef> triangles() { 
		return Collections.unmodifiableList(this.objectGround) ; 
	}
	
	
	
	
	
	public static  class ObjectNavmeshBuilder { 
		
		private ObjectNavMesh bms  = new ObjectNavMesh() ; 
		
	
		public ObjectNavmeshBuilder id(int id) { 
			bms.id = id ; 
 			return this ; 
		}
	 	public ObjectNavmeshBuilder	point(float x , float z ,  float y ) { 
		  
	 		bms.points.add(new Position(x, z, y)); 
	 	
			return this ; 
		}
		
	 	public ObjectNavmeshBuilder objectGround(short pointA , short pointB , short pointC , short ukn) { 
	 		
	 		if(ukn != 0) throw new IllegalArgumentException() ; 
	 		
	 		bms.objectGround.add(new ObjectGroundTriRef(pointA, pointB, pointC, ukn)) ;
	 		
	 		return this ; 
	 	}
	 	
		public ObjectNavmeshBuilder inLine(short pointA  , short pointB ,
				short neighbourA ,short neighbourB  , byte flag ) {
			ObjectLineRef line = new ObjectLineRef(pointA, pointB, neighbourA, neighbourB, flag );
			bms.inLines.add(line) ; 
			
			if(flag == 7 ) { 
				bms.inCanBlock = true ; 
			}
			
			if(neighbourA != 0xffffffff) { 
				//bms.objectGround.get(neighbourA).addNeighbour((short) (bms.inLines.size() - 1));
				bms.objectGround.get(neighbourA).addInLineNeighbour((short) (bms.inLines.size() - 1));
				
			}
			if(neighbourB != 0xffffffff) { 
				//bms.objectGround.get(neighbourB).addNeighbour((short) (bms.inLines.size() - 1));
				bms.objectGround.get(neighbourA).addInLineNeighbour((short) (bms.inLines.size() - 1));
			}
			return this ; 
		}
		
		public ObjectNavmeshBuilder outLine(short pointA  , short pointB ,
				short neighbourA ,short neighbourB  , byte flag ) { 
			
			ObjectLineRef line = new ObjectLineRef(pointA, pointB, neighbourA, neighbourB, flag ); 
			bms.outLines.add(line);
			
			if(flag == 3) { 
				bms.outCanBlock = true ; 
			}else if(flag == 0) { 
				bms.hasEntrance = true ; 
			}
			
			if(neighbourA != 0xffffffff ) { 
				//bms.objectGround.get(neighbourA).addNeighbour((short) (bms.outLines.size() - 1));
				bms.objectGround.get(neighbourA).addOutLineNeighbour((short) (bms.outLines.size() - 1));
			}
			
			if(neighbourB != 0xffffffff) { 
				//bms.objectGround.get(neighbourB).addNeighbour((short) (bms.outLines.size() - 1));
				bms.objectGround.get(neighbourB).addOutLineNeighbour((short) (bms.outLines.size() - 1));
			}
			
			return this ; 
		}
		
		
		public ObjectNavMesh build() { 
			return this.bms ; 
		}
		
	}
	
	
	
	
	
	
	
	

	
	
	
	
	
	

	
	public enum InlineFlag { 
		Passable(4) , 
		Block1(7) , 
		PassableIfNotOnObject(20) , 
		Block2(135) ;
		
		private byte val ;
		
		
		private InlineFlag(int val) { 
			this.val = (byte)val ; 
		}
		
		public byte getValue() { 
			return this.val ; 
		}
		
	}
	
	
	public enum OutLineType { 
		
		
		Entrance(0) , 
		Block1(3) ,  
		Passable(8) , 
		PassableIfNotOnObject(16) , 
		Block2(131)  ; 
		
		
		private byte val ; 
		
		private OutLineType(int val) { 
			this.val =(byte) val ; 
			
		}
		
		public byte getValue() { 
			return this.val ; 
		}
		
	}
}
