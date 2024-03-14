package org.sokybot.machinegroup.gamemodel.item;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Delegate;

@Entity
@Getter
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
	
	
	public ShopItem(ItemEntity itemEntity , int slot , int price) { 
		this.entity = itemEntity ; 
		this.slot = slot ; 
		this.price = price ; 
	}
	
	
}

