package org.sokybot.machine.event.trainerevent;

import org.sokybot.machine.gamemodel.Trainer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TrainerLoadedEvent {

	private Trainer traienr ; 


	public Trainer getTrainer() { 
		return this.traienr ; 
	}
}
