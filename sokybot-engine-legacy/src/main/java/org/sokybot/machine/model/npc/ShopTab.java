package org.sokybot.machine.model.npc;

import java.util.Map;

import org.sokybot.persistence.entities.SilkroadEntity;
import org.sokybot.machine.model.item.ShopItem;

import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;




@SuperBuilder
@ToString(callSuper =  true)
public class ShopTab extends SilkroadEntity{

	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int groupId ; 
	
	@Singular
	private Map<Integer, ShopItem> tabItems ; 

	
	
	public ShopItem[] items() { 
		return this.tabItems.values().toArray((len)->new ShopItem[len]) ; 
	}
	
	

}
