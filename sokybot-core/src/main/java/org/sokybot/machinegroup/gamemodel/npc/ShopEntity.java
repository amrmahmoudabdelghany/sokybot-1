package org.sokybot.machinegroup.gamemodel.npc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.sokybot.machinegroup.gamemodel.item.ShopItem;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Delegate;

@Entity
@Getter
@ToString
@NoArgsConstructor
public class ShopEntity implements Serializable {

	@Id
	private int shopId;

	@Delegate
	@OneToOne
	private NPCEntity npcEntity;
	
	//private int shopNpcId ; 

	@Getter(value =  AccessLevel.NONE)
	@ElementCollection(fetch = FetchType.EAGER)
	private Map<Integer, Byte> tabNumbers = new HashMap<>() ; 
	
	
	public ShopEntity(int shopId, NPCEntity npcEntity, ShopTab... tabs) {
		this.shopId = shopId;
		this.npcEntity = npcEntity ; 
		///this.shopNpcId = npcId  ; 
		byte tabNum = 0 ; 
		
		ShopTab tab = null ;
		while(( tab = tabs[tabNum]) != null) {
			
			ShopItem[] items = tab.items() ; 
			for(ShopItem item : items) { 
				tabNumbers.put(item.getRefId(), tabNum) ; 
			}
			tabNum++; 
		}
	}
	
	
	public Optional<Byte> getItemTabNumber(int itemRefId) { 
		return Optional.ofNullable(tabNumbers.get(itemRefId)) ;
	}
	
	
	
	
	

}
