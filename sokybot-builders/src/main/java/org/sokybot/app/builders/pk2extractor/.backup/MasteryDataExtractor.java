package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.transform.ToListResultTransformer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.transform.ToListResultTransformer;
import org.sokybot.persistence.entities.MasteryData;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.persistence.service.MasteryDataRepoRepository;
import org.sokybot.pk2.IPk2Driver;







import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component(service = IExtractor.class)
@Qualifier("mediapk2")
public class MasteryDataExtractor implements IExtractor {

	
	private MasteryDataRepository masteryDataRepository ; 
	
	private Cache cache ; 
	
	@Reference
	public MasteryDataExtractor(CacheManager cacheManager , MasteryDataRepository repo) {
		this.cache = cacheManager.getCache(CACHE_NAME) ; 
		this.masteryDataRepository = repo ; 
		
	}
	
	
	@Override
	public void extract(IPk2Driver driver) {
	 
		log.info("extracting masteries data");
		driver.findFirst("skillmasterydata.txt").ifPresent((jmx) -> {
		 List<MasteryData> list = 	Pk2ExtractorUtils.toCSVRecordStream(jmx).filter((record) -> {

				if (record.size() < 3) {
					return false;
				}
				if (!NumberUtils.isParsable(record.get(0))) {
					return false;
				}

				if (record.get(2).contains("xxx")) {
					return false;
				}
				return true;
			}).map((record) -> {
				String masteryId =  record.get(0);

				String UIITName = record.get(2);

				//System.out.println("Key : " + key + " value : " + value) ; 
				String name = this.cache.get(UIITName, String.class);
				  name = (name == null) ? UIITName : name ; 
 
				  return new MasteryData(Integer.parseInt(masteryId) , name); 
					  

			})		.collect(Collectors.toList()) ; 
		 
		 this.masteryDataRepository.saveAll(list) ; 
		});
		
	}

}




