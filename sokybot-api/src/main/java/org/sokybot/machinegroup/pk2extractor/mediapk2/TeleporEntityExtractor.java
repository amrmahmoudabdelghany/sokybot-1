package org.sokybot.machinegroup.pk2extractor.mediapk2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.machinegroup.gamemodel.portal.TeleportEntity;
import org.sokybot.machinegroup.pk2extractor.IExtractor;
import org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.machinegroup.repo.TeleportEntityRepo;
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
@Order(5)
@Scope("prototype")
@Qualifier("mediapk2")
public class TeleporEntityExtractor implements IExtractor {

	private TeleportEntityRepo teleportEntityRepo;

	private Cache cache;

	private Map<Integer, List<Integer>> links = new HashMap<>();

	@Autowired
	public TeleporEntityExtractor(CacheManager cacheManager, TeleportEntityRepo repo) {
		this.cache = cacheManager.getCache(CACHE_NAME);
		this.teleportEntityRepo = repo;
	}

	private void extractLinks(IPk2Driver driver) {

		log.info("extracting links from teleportlink.txt");
		driver.find("teleportlink.txt").forEach((jmx) -> {
			Pk2ExtractorUtils.toCSVRecordStream(jmx).forEach((record) -> {
				String field = record.get(1);

				int id = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
				field = record.get(2);
				int link = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
				List<Integer> linkArr = links.get(id);
				if (linkArr == null) {
					linkArr = new ArrayList<>();
					links.put(id, linkArr);
				}
				linkArr.add(link);
			});
		});

	}

	@Override
	public void extract(IPk2Driver driver) {
		extractLinks(driver);

		log.info("extracting teleport data from teleportdata.txt");
		// Map<Integer, TeleportEntity> teleport = new HashMap<>();
		driver.find("teleportdata.txt").forEach((teleportdata) -> {
		List<TeleportEntity> teleports = 	Pk2ExtractorUtils.toCSVRecordStream(teleportdata).map((record) -> {

				String field = record.get(1);
				int refId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;

				String longId = record.get(2);

				field = record.get(3);

				int buildId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;

				field = record.get(4);
				// String name = this.cacheStorage.getValueOrDefault(field, String.class,
				// field);
				String name = this.cache.get(field, String.class);
				name = (name == null) ? field : name;

				TeleportEntity entity = TeleportEntity.builder()
						.refId(refId)
						.longId(longId)
						.portalId(buildId)
						.name(name)
						.links(links.getOrDefault(refId, new ArrayList<>()))
						.build();

 
				return entity ;
			}).collect(Collectors.toList()) ; 
		  
		this.teleportEntityRepo.saveAll(teleports);

		});

	}

}
