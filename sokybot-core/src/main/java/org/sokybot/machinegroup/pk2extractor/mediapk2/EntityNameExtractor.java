package org.sokybot.machinegroup.pk2extractor.mediapk2;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.sokybot.machinegroup.pk2extractor.IExtractor;
import org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2.IPk2Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(1)
@Component
@Scope("prototype")
@Qualifier("mediapk2")
public class EntityNameExtractor implements IExtractor {

	private Cache cache ; 
	
	
	@Autowired
	public EntityNameExtractor(CacheManager cacheManager ) {
		
		this.cache = cacheManager.getCache(CACHE_NAME) ; 
		
	}
	
	
	@Override
	public void extract(IPk2Driver driver) {
	 
		
		log.info("extracting entity names from media pk2 file ");
		driver.find("((?i)(textdata_equip&skill(_\\d*?)?(\\.txt))|(textdata_object(_\\d*?)?(\\.txt)))")
				.stream()
				.flatMap(Pk2ExtractorUtils::toCSVRecordStream)
				.filter((record) -> {
					if (record.size() < 2 )
						return false;

					String firstField = record.get(0);

					return !firstField.startsWith("//") && !firstField.equals("0") && !firstField.isBlank();
				})
				.collect(Collectors.toMap((record) -> {

					for (int i = 1; i < record.size(); i++) {
						String str = record.get(i);
						if (str.contains("SN_") || str.contains("UIIT_")) {
							return record.get(i);
						}
					}
					return "UNDEFINED";

				}, (record) -> {

					for (int i = 2; i < record.size(); i++) {
						String val = record.get(i);

						if (!val.contains("SN_") && !val.contains("UIIT_") && Pattern.matches(".*[a-zA-Z]+.*", val)) {
							return record.get(i);

						}

					}

					return "UNDEFINED";

				}, (name1, name2) -> {

					return name2;
				}))
				.forEach(cache::put) ; 
		

	}
}
