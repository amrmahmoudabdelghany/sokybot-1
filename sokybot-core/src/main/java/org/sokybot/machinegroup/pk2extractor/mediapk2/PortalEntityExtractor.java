package org.sokybot.machinegroup.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.machinegroup.gamemodel.portal.PortalEntity;
import org.sokybot.machinegroup.gamemodel.portal.TeleportEntity;
import org.sokybot.machinegroup.pk2extractor.IExtractor;
import org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.machinegroup.repo.PortalEntityRepo;
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
@Component
@Order(6)
@Scope("prototype")
@Qualifier("mediapk2")
public class PortalEntityExtractor implements IExtractor {

	private PortalEntityRepo portalEntityRepo ; 
	
	private Cache cache ; 
	
	
	@Autowired
	public PortalEntityExtractor(CacheManager cacheManager , PortalEntityRepo repo) {
		this.cache = cacheManager.getCache(CACHE_NAME) ; 
		this.portalEntityRepo = repo ; 
	}
	
	
	
	@Override
	public void extract(IPk2Driver driver) {
		log.info("extracting portals from teleportbuilding.txt");
		driver.find("teleportbuilding.txt").forEach((teleportbuilding) -> {
		List<PortalEntity> portals = 	Pk2ExtractorUtils.toCSVRecordStream(teleportbuilding).map((record) -> {

				String field = record.get(1);
				int refId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
				String longId = record.get(2);
				field = record.get(5);
				//String name = this.cacheStorage.getValueOrDefault(field, String.class, field);
				String name = this.cache.get(field, String.class) ; 
				name = (name == null) ? field : name ; 
				
				//TeleportEntity entity = teleport.get(refId);
				PortalEntity portal = PortalEntity.builder()
						.refId(refId)
						.longId(longId)
						.name(name)
					//	.teleport(entity)
						.build();

				return portal ; 
				
				//String key = String.valueOf(portal.getRefId());
				//store(key, portal);
				// this.cacheStorage.store(String.valueOf(portal.getRefId()), portal) ;

			}).collect(Collectors.toList()) ; 
		  
		this.portalEntityRepo.saveAll(portals) ; 
		
		});
	}
}
