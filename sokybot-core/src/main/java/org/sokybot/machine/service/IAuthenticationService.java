package org.sokybot.machine.service;


public interface IAuthenticationService {

	
	public void discoverAgents();
	public void login(String userName , String password , short agentId) ; 
	public void authenticate(String userName , String password , int loginId ) ; 
	public void logout(); 
	
}
