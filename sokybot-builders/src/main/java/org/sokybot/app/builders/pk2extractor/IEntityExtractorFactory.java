import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor;

import org.sokybot.machinegroup.service.IDataPk2;
import org.sokybot.machinegroup.service.IMediaPk2;

public interface IEntityExtractorFactory {
	
	
	public IMediaPk2 getMediaPk2() ; 
	public IDataPk2  getDataPk2() ; 
	
	

}




