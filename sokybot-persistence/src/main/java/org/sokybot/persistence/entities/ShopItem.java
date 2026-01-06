package org.sokybot.persistence.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Delegate;

@Entity
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ShopItem {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id  ; 
	
	private int slot ; 
	
	@Delegate
	@OneToOne
	private ItemEntity entity ; 
	private int price ;
	
	
	public int getId() { return id; }
	public int getSlot() { return slot; }
	public ItemEntity getEntity() { return entity; }
	public int getPrice() { return price; }
	
	public void setId(int id) { this.id = id; }
	public void setSlot(int slot) { this.slot = slot; }
	public void setEntity(ItemEntity entity) { this.entity = entity; }
	public void setPrice(int price) { this.price = price; }
	
	
	public ShopItem(ItemEntity itemEntity , int slot , int price) { 
		this.entity = itemEntity ; 
		this.slot = slot ; 
		this.price = price ; 
	}
	
	
}
