package org.sokybot.machinegroup.pk2extractor.mediapk2;

import java.util.List;

import org.sokybot.machinegroup.pk2extractor.IExtractor;
import org.sokybot.pk2.IPk2Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Scope("prototype")
@Slf4j
public class MediaPk2Extractor  implements IExtractor{

	
	@Autowired
	@Qualifier("mediapk2")
	private List<IExtractor> mediaExtractors ; 
	
	@Override
	public void extract(IPk2Driver driver) {
	 
		log.info("Starting extract data from Media.pk2");
		 for(IExtractor ex : mediaExtractors) { 
			 ex.extract(driver);
		 }
		 log.info("extring data from Media.pk2 done");
		
	}
}
