package org.sokybot.machinegroup.pk2extractor.mediapk2;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.machinegroup.gamemodel.skill.SkillEntity;
import org.sokybot.machinegroup.gamemodel.skill.SkillType;
import org.sokybot.machinegroup.pk2extractor.IExtractor;
import org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.machinegroup.repo.SkillEntityRepo;
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
public class SkillEntityExtractor implements IExtractor {

	
	private SkillEntityRepo skillEntityRepo ; 
	
	private Cache cache ; 
	
	@Autowired
	public SkillEntityExtractor(CacheManager cacheManager , SkillEntityRepo repo) {
		this.cache = cacheManager.getCache(CACHE_NAME) ; 
		this.skillEntityRepo = repo ; 
		
	}
	
	
	
	@Override
	public void extract(IPk2Driver driver) {

		log.info("extracting skill entities from media.pk2 file");
	  List<SkillEntity> skills = 	 driver.find("skilldata_(\\d+)(enc)?.txt$")
				.stream()
				.flatMap(Pk2ExtractorUtils::toCSVRecordStream)
				.filter((r) -> r.size() > 70)
				.map(this::toSkillEntity)
				.distinct()
				.collect(Collectors.toList()) ; 
	  
	  this.skillEntityRepo.saveAll(skills) ; 
				
				//.forEach((skill) -> {
				///	this.cache.put(skill.getLongId(), skill.getRefId()) ; 					
				//	this.skillEntityRepo.save(skill) ; 
				//});

	}
	
	private SkillEntity toSkillEntity(CSVRecord record) {

		var builder = SkillEntity.builder();

		String field = record.get(1);
		if (NumberUtils.isParsable(field)) {

			builder.refId(Integer.parseInt(field));
		}

		String longId = record.get(3);

		builder.longId(longId);

		field = record.get(13); // castTime
		if (NumberUtils.isParsable(field)) {
			builder.castTime(Integer.parseInt(field));
		}

		field = record.get(14); // cooldown
		if (NumberUtils.isParsable(field)) {
			builder.cooldown(Integer.parseInt(field));
		}

		field = record.get(22);
		boolean targetRequired = false;

		if (NumberUtils.isParsable(field)) {
			targetRequired = BooleanUtils.toBoolean(Byte.valueOf(field));
		}

		builder.targetRequired(targetRequired);

		field = record.get(34);
		if (NumberUtils.isParsable(field)) {
			builder.masteryId(Integer.parseInt(field));
		}

		field = record.get(53);
		if (NumberUtils.isParsable(field)) {
			builder.MP(Integer.parseInt(field));
		}

		field = record.get(62); // SN_NAME
		String name = this.cache.get(field, String.class) ; 
		
		builder.name((name == null) ? field : name);

		field = record.get(70);
		if (NumberUtils.isParsable(field)) {
			builder.duration(Integer.parseInt(field));

		}

		// parse type
		SkillType type = SkillType.Passive;

		field = record.get(8); // {0 , 1 , 2} 0 = passive

		if (!field.equals("0")) {
			type = SkillType.parseType(longId);
		}

		builder.type(type);

		return builder.build();
	}

}
