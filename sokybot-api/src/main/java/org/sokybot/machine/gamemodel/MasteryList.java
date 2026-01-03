package org.sokybot.machine.gamemodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sokybot.common.ListChangeListener;
import org.sokybot.common.ListChangeSupport;
import org.sokybot.common.ObservableList;
import org.sokybot.machinegroup.gamemodel.skill.Mastery;

public class MasteryList {

	private ObservableList<Mastery> changeSupport = new ListChangeSupport<>();

	private List<Mastery> masteries = new ArrayList<>();

	protected void addMastery(Mastery mastery) {
		if (this.masteries.add(mastery))
			changeSupport.fireItemAddedEvent(masteries.size() - 1, mastery);

	}

	protected void removeMastery(Mastery mastery) {
		if (this.masteries.remove(mastery)) {
			changeSupport.fireItemRemovedEvent(mastery);
		}
	}

	public void setMasteryLevel(int masteryId, byte lvl) {
		this.masteries.stream().filter((m) -> m.getMasteryID() == masteryId).findFirst().ifPresent((mastery) -> {
			mastery.setMasteryLevel(lvl);
			changeSupport.fireItemUpdatedEvent(mastery);
		});

	}

	public void clear() { 
		this.masteries.clear();
	}
	public Mastery getMasteryAt(int index) { 
		
		return this.masteries.get(index) ; 
	}
	public Optional<Mastery> findMastry(String name) {
		return this.masteries.stream().filter((m) -> m.getName().equals(name)).findFirst();
	}

	public Optional<Mastery> findMastry(int id) {
		return this.masteries.stream().filter((m) -> m.getMasteryID() == id).findFirst();
	}

	public List<Mastery> getAllMasteries() {

		return Collections.unmodifiableList(this.masteries);
	}
	
	public int count() { 
		return this.masteries.size() ; 
	}

	public void addMastryListListener(ListChangeListener<Mastery> listener) {

		if (listener != null)
			this.changeSupport.addListChangeListener(listener);
	}

	public void removeMasteryListListener(ListChangeListener<Mastery> listener) {
		if (listener != null) {
			this.changeSupport.removeListChangeListner(listener);
		}
	}

}
