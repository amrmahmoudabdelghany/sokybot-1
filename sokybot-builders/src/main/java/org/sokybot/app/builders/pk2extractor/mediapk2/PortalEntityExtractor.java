import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.machinegroup.gamemodel.portal.PortalEntity;
import org.sokybot.machinegroup.gamemodel.portal.TeleportEntity;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.persistence.service.PortalEntityRepoRepository;
import org.sokybot.pk2.IPk2Driver;








import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = IExtractor.class)
public class PortalEntityExtractor implements IExtractor {

	private PortalEntityRepository portalEntityRepository ; 
	
	private Cache cache ; 
	
	
	@Reference
	public PortalEntityExtractor(CacheManager cacheManager , PortalEntityRepository repo) {
		this.cache = cacheManager.getCache(CACHE_NAME) ; 
		this.portalEntityRepository = repo ; 
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
		  
		this.portalEntityRepository.saveAll(portals) ; 
		
		});
	}
}




