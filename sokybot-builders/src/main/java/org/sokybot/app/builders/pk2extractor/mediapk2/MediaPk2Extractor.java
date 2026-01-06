import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;

import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.pk2.IPk2Driver;





import lombok.extern.slf4j.Slf4j;

@Component(service = IExtractor.class)
@Slf4j
public class MediaPk2Extractor  implements IExtractor{

	
	@Reference
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




