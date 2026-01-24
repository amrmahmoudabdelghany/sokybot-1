package org.sokybot.persistence.entities;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.CollectionTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.Column;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ShopTab extends SilkroadEntity {

	private static final long serialVersionUID = 1L;

	private int groupId ; 
	
	@Singular
	@ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="shop_tab_items")
    @MapKeyColumn(name="item_key")
    @Column(name="shop_item")
	private Map<Integer, ShopItem> tabItems ; 
	
	public int getGroupId() {
		return groupId;
	}
	
	public Map<Integer, ShopItem> getTabItems() {
		return tabItems;
	}
	
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
	
	public void setTabItems(Map<Integer, ShopItem> tabItems) {
		this.tabItems = tabItems;
	}

	public ShopItem[] items() { 
		return this.tabItems.values().toArray((len)->new ShopItem[len]) ; 
	}

}
