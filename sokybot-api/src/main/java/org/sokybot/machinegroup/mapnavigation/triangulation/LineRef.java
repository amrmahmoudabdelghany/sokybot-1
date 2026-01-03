package org.sokybot.machinegroup.mapnavigation.triangulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sokybot.machinegroup.mapnavigation.triangulation.CellSplitRef.LineType;

import lombok.Getter;
import lombok.ToString;


// this class must be immutable 
@Getter
@ToString
public  class LineRef  { 
	
	
	
	protected short aIndex , bIndex  ;
	
	protected LineType lineType = LineType.NonBlock ; 
	protected List<Short> neighbors = new ArrayList<>() ; 
	protected short extInfo ; 
	
	public LineRef(short a , short b) { 
		this.aIndex = a ; 
		this.bIndex = b ; 
	}
	
	

	
	
	@Override
	public int hashCode() {
		return aIndex + bIndex ; 
	}


	@Override
	public boolean equals(Object obj) {
	//	if (this == obj)
		//	return true;
	//	if (obj == null)
	//		return false;
	//	if (getClass() != obj.getClass())
	//		return false;
		LineRef other = (LineRef) obj;
		return(aIndex == other.aIndex && bIndex == other.bIndex) || (aIndex == other.bIndex && bIndex == other.aIndex) ;
	}



	
	
	
}
