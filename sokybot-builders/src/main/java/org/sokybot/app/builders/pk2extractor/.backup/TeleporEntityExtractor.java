import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.machinegroup.gamemodel.portal.TeleportEntity;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.persistence.service.TeleportEntityRepoRepository;
import org.sokybot.pk2.IPk2Driver;








import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = IExtractor.class)
public class TeleporEntityExtractor implements IExtractor {

	private TeleportEntityRepository teleportEntityRepository;

	private Cache cache;

	private Map<Integer, List<Integer>> links = new HashMap<>();

	@Reference
	public TeleporEntityExtractor(CacheManager cacheManager, TeleportEntityRepository repo) {
		this.cache = cacheManager.getCache(CACHE_NAME);
		this.teleportEntityRepository = repo;
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
		  
		this.teleportEntityRepository.saveAll(teleports);

		});

	}

}




