package org.sokybot.persistence.entities;

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

import org.sokybot.persistence.entities.ShopItem;
import org.sokybot.persistence.entities.ShopTab;

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
	
	
	public int getShopId() {
		return shopId;
	}
	
	public NPCEntity getNpcEntity() {
		return npcEntity;
	}
	
	public Map<Integer, Byte> getTabNumbers() {
		return tabNumbers;
	}
	
	public void setShopId(int shopId) {
		this.shopId = shopId;
	}
	
	public void setNpcEntity(NPCEntity npcEntity) {
		this.npcEntity = npcEntity;
	}
	
	public void setTabNumbers(Map<Integer, Byte> tabNumbers) {
		this.tabNumbers = tabNumbers;
	}
	
	
	public ShopEntity(int shopId, NPCEntity npcEntity, ShopTab... tabs) {
		this.shopId = shopId;
		this.npcEntity = npcEntity ; 
		///this.shopNpcId = npcId  ; 
		byte tabNum = 0 ; 
		
		ShopTab tab = null ;
		int i = 0;
		// Fix potentially infinite loop or array index usage if tabs is varargs
		// Use for-each or standard loop
		if (tabs != null) {
			for(ShopTab t : tabs) {
				if(t == null) continue;
				ShopItem[] items = t.items() ; 
				if (items != null) {
					for(ShopItem item : items) { 
						tabNumbers.put(item.getId(), tabNum) ; 
					}
				}
				tabNum++; 
			}
		}
	}
	
	
	public Optional<Byte> getItemTabNumber(int itemRefId) { 
		return Optional.ofNullable(tabNumbers.get(itemRefId)) ;
	}
	
	
	
	
	

}
