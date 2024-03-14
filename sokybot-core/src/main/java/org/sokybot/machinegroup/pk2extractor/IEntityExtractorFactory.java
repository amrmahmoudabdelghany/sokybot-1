package org.sokybot.machinegroup.pk2extractor;

import org.sokybot.machinegroup.service.IDataPk2;
import org.sokybot.machinegroup.service.IMediaPk2;

public interface IEntityExtractorFactory {
	
	
	public IMediaPk2 getMediaPk2() ; 
	public IDataPk2  getDataPk2() ; 
	
	

}
