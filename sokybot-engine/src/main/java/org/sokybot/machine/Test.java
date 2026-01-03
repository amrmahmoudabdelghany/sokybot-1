
package org.sokybot.machine;

import org.sokybot.machine.event.trainerevent.TrainerLoadedEvent;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machinegroup.gamemodel.item.Inventory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class Test {

	
	
	@EventListener
	public void onTrainerLoaded(TrainerLoadedEvent ev) { 
		Trainer t = ev.getTraienr() ; 
		Inventory inv = t.getItemInventory() ;
		System.out.println("Trainer Inventory is : "  ) ; 
		System.out.println(inv) ; 
		
		
	}
}

