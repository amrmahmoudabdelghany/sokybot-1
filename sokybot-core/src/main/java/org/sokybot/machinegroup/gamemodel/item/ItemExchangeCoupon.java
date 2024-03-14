package org.sokybot.machinegroup.gamemodel.item;

import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.ToString;



@ToString(callSuper =  true)
@EqualsAndHashCode(callSuper = true)
public class ItemExchangeCoupon extends Item {

	private List<Long> magParamValues = new ArrayList<>() ; 
	
	public ItemExchangeCoupon(Item item) {
		super(item);
	} 
	
	public void addMagParamValue(long magParam) { 
		this.magParamValues.add(magParam); 
	}
	
	
	
	
	
}