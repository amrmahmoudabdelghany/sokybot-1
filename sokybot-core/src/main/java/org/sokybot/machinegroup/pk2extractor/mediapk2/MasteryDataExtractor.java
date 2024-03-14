package org.sokybot.machinegroup.pk2extractor.mediapk2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.transform.ToListResultTransformer;
import org.sokybot.machinegroup.gamemodel.MasteryData;
import org.sokybot.machinegroup.pk2extractor.IExtractor;
import org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.machinegroup.repo.MasteryDataRepo;
import org.sokybot.pk2.IPk2Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@Scope("prototype")
@Qualifier("mediapk2")
public class MasteryDataExtractor implements IExtractor {

	
	private MasteryDataRepo masteryDataRepo ; 
	
	private Cache cache ; 
	
	@Autowired
	public MasteryDataExtractor(CacheManager cacheManager , MasteryDataRepo repo) {
		this.cache = cacheManager.getCache(CACHE_NAME) ; 
		this.masteryDataRepo = repo ; 
		
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
		 
		 this.masteryDataRepo.saveAll(list) ; 
		});
		
	}

}
