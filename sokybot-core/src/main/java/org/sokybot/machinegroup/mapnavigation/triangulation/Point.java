package org.sokybot.machinegroup.mapnavigation.triangulation;



import java.io.Serializable;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;




@Getter
@NoArgsConstructor
@ToString(callSuper = true)
public class Point  implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id  ;
	
	private byte flag;
	private double z;
	private double x ; 
	private double y ; 
	
	
	public Point(double x , double y ) { 
		this.x  = x ; 
		this.y = y ; 
		this.z = 0 ; 
	}
	public Point(double x, double z, double y) {
		this.x = x;
		this.z = z;
		this.y = y;
	}

	
	
	@Override
	public String toString() { 
		return "(" + x + "," + y + ")" ; 
	}
}