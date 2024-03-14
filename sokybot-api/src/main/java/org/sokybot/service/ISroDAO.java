package org.sokybot.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ISroDAO {
	

	String getGamePath() ; 

	Map<String, List<String>> getDivHosts() ; 
	
	Optional<String> getRndHost();
	Optional<Byte> getLocal() ; 
	Optional<String> getLanguage() ; 
	Optional<String> getCountry() ; 
	int getPort();
    int getVersion();

}
