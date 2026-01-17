package org.sokybot.machine.model.item;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true) 
@EqualsAndHashCode(callSuper =  true)
public class AbilityPet extends PetScroll {

	private int secondsToRentEndTime;

	public AbilityPet(Item item) {
		super(item);
	}
}
