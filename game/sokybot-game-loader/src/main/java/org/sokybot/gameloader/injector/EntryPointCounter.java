package org.sokybot.gameloader.injector;

public class EntryPointCounter implements IEntryPointCounter {

	private int relative = -1 ; 
	
	
	
	
	@Override
	public void encounter(int index) {
	 
			this.relative = index ; 	
	}
	
	@Override
	public int getRelativeEntryPoint() {
		return this.relative ; 
	}
	
	
}
