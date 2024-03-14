package org.sokybot.common;

public interface ListChangeListener<T> {

	 
	public void onItemAdded(int index , T item) ; 
	public void onItemRemoved(T item) ; 
	public void onItemUpdated(T item); 
}
