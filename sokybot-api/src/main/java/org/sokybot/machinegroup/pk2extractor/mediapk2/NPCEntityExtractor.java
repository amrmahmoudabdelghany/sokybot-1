package org.sokybot.machinegroup.pk2extractor.mediapk2;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.machinegroup.gamemodel.npc.NPCEntity;
import org.sokybot.machinegroup.gamemodel.npc.NPCType;
import org.sokybot.machinegroup.pk2extractor.IExtractor;
import org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.machinegroup.pk2extractor.exception.Pk2MissedResourceException;
import org.sokybot.machinegroup.repo.NPCEntityRepo;
import org.sokybot.pk2.IPk2Driver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(4)
@Scope("prototype")
@Qualifier("mediapk2")
public class NPCEntityExtractor implements IExtractor {

	private NPCEntityRepo npcEntityRepo;

	private Cache cache;

	public NPCEntityExtractor(CacheManager cacheManager, NPCEntityRepo repo) {
		this.cache = cacheManager.getCache(CACHE_NAME);
		this.npcEntityRepo = repo;
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
				  
	  this.npcEntityRepo.saveAll(list) ; 
	             
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
