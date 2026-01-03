package org.sokybot.common;

public interface ObservableList<T> {

	public void addListChangeListener(ListChangeListener<T> listener) ; 
	public void removeListChangeListner(ListChangeListener<T> listener) ;
	public void fireItemAddedEvent( int x , T item) ; 
	public void fireItemRemovedEvent(T item) ; 
	public void fireItemUpdatedEvent(T item) ; 
}
