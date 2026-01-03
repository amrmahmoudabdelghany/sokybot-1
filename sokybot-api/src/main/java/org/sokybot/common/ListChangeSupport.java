package org.sokybot.common;

import java.util.ArrayList;
import java.util.List;

public class ListChangeSupport<T> implements ObservableList<T>{

	private List<ListChangeListener<T>> listeners = new ArrayList<>() ; 
	
	
	@Override
	public void addListChangeListener(ListChangeListener<T> listener) {
	  this.listeners.add(listener) ; 	
	}

	@Override
	public void removeListChangeListner(ListChangeListener<T> listener) {
		this.listeners.remove(listener);
	}

	@Override
	public void fireItemAddedEvent(int x , T item) {
		for(ListChangeListener<T> listener : listeners) { 
			listener.onItemAdded(x, item);
			
		}
	}

	@Override
	public void fireItemRemovedEvent(T item) {
		for(ListChangeListener<T> listener : listeners) { 
			listener.onItemRemoved(item);
		}
		 
	}

	@Override
	public void fireItemUpdatedEvent(T item) {
		for(ListChangeListener<T> listener : listeners) { 
			listener.onItemUpdated(item);
		}

		
	}


}
