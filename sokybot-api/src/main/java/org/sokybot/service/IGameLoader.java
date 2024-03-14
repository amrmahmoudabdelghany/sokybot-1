package org.sokybot.service;


public interface IGameLoader {

	
	
	public int launch(String clientPath ) ; 
	/**
	 * 
	 * 
	 * @param clientPath actual path to a client 
	 * @param command  command that passed to the client
	 * @return Process handle that can be used to manipulate the opened process
	 */
	public int launch(String clientPath , String command) ; 
	
	/**
	 * return unique name for this implementation that used to identify it 
	 * 
	 * @return unique name
	 */
	public String getName() ;
	
	/**
	 * 
	 * -1 indicate unlimited
	 * @return minimum supported game version
	 */
	public Integer minimumVersion() ; 
	
	/**
	 * 
	 * -1 indicate unlimited
	 * 
	 * @return maximum supported game version 
	 */
	public Integer maximumVersion() ; 
	
}
