package org.sokybot.app.service;

import java.util.Optional;
import java.util.Set;

import org.sokybot.service.IGameLoader;

public interface IGameLoaderService {

	
	Set<String> listAvailables() ; 
	
	Optional<IGameLoader> findGameLoader(String name) ; 
	
}
