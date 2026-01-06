import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor;

import org.sokybot.pk2.IPk2Driver;

public interface IExtractor { 
	
	
	public static final String CACHE_NAME  = "extraction-cache" ; 

	void extract(IPk2Driver driver) ; 
}




