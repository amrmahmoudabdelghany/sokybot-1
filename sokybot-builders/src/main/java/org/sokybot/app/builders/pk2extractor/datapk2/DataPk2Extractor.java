import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.datapk2;

import java.util.List;

import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.mediapk2.MediaPk2Extractor;
import org.sokybot.pk2.IPk2Driver;





import lombok.extern.slf4j.Slf4j;



@Component(service = IExtractor.class)
@Slf4j
public class DataPk2Extractor implements IExtractor {

	

	@Reference
	@Qualifier("datapk2")
	private List<IExtractor> dataExtractors ; 
	
	
	@Override
	public void extract(IPk2Driver driver) {
		log.info("Starting extract data from Data.pk2");
		 for(IExtractor ex : dataExtractors) { 
			 ex.extract(driver);
		 }
		 log.info("extring data from Data.pk2 done");
		
		
	}
}




