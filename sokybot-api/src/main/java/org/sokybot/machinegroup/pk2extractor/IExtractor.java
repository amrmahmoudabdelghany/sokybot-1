package org.sokybot.machinegroup.pk2extractor;

import org.sokybot.pk2.IPk2Driver;

public interface IExtractor { 
	
	
	public static final String CACHE_NAME  = "extraction-cache" ; 

	void extract(IPk2Driver driver) ; 
}
