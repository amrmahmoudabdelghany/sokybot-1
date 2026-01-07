import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.mediapk2;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.machinegroup.gamemodel.npc.NPCEntity;
import org.sokybot.persistence.entities.NPCType;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.exception.Pk2MissedResourceException;
import org.sokybot.persistence.service.NPCEntityRepoRepository;
import org.sokybot.pk2.IPk2Driver;







import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = IExtractor.class)
public class NPCEntityExtractor implements IExtractor {

	private NPCEntityRepository npcEntityRepository;

	private Cache cache;

	public NPCEntityExtractor(CacheManager cacheManager, NPCEntityRepository repo) {
		this.cache = cacheManager.getCache(CACHE_NAME);
		this.npcEntityRepository = repo;
	}

	@Override
	public void extract(IPk2Driver driver) {

		log.info("extracting NPC entities from media.pk2 file");

	  List<NPCEntity> list = 	driver.findFirst("characterdata.txt")
				.map(jmx -> Pk2ExtractorUtils.toString(jmx, StandardCharsets.UTF_16.name()))
				.map(Pk2ExtractorUtils::toLines)
				.orElseThrow(() -> new Pk2MissedResourceException("Colud not find characterdata.txt file",
						"characterdata.txt"))
				.flatMap((npcFileName) -> driver.find("(?i)" + npcFileName).stream())
				.flatMap(Pk2ExtractorUtils::toCSVRecordStream)
				.map(this::toNPCEntity)
				.peek((ele)->cache.put(ele.getLongId(), ele.getRefId()))
				.collect(Collectors.toList()) ; 
				  
	  this.npcEntityRepository.saveAll(list) ; 
	             
	}

	private NPCEntity toNPCEntity(CSVRecord record) {
		String field = record.get(1);
		int refId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;

		field = record.get(5);
	//	String name = this.cacheStorage.getValueOrDefault(field, String.class, field);
		String name = this.cache.get(field, String.class) ; 
		name = (name == null) ? field : name ; 
		
		field = record.get(57);
		int lvl = NumberUtils.isParsable(field) ? Integer.parseInt(field) : 0;

		field = record.get(59);
		int hp = NumberUtils.isParsable(field) ? Integer.parseInt(field) : 0;

		String typeFlag = record.get(10) + record.get(11) + record.get(12) + record.get(14) + record.get(15);
		NPCType type = NPCType.of(NumberUtils.isParsable(typeFlag) ? Integer.parseInt(typeFlag) : -1);

		return NPCEntity.builder().refId(refId).longId(record.get(2)).name(name).level(lvl).HP(hp).Type(type).build();

	}

}




