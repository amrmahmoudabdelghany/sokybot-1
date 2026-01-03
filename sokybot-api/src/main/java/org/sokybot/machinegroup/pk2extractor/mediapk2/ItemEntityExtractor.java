package org.sokybot.machinegroup.pk2extractor.mediapk2;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.machinegroup.gamemodel.Gender;
import org.sokybot.machinegroup.gamemodel.Race;
import org.sokybot.machinegroup.gamemodel.item.ItemEntity;
import org.sokybot.machinegroup.gamemodel.item.ItemType;
import org.sokybot.machinegroup.pk2extractor.IExtractor;
import org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.machinegroup.pk2extractor.exception.Pk2MissedResourceException;
import org.sokybot.machinegroup.repo.ItemEntityRepo;
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
@Order(3)
@Scope("prototype")
@Qualifier("mediapk2")
public class ItemEntityExtractor implements IExtractor {
 
	
	private Cache cache ; 
	private ItemEntityRepo repo ; 
	
	@Autowired
	public ItemEntityExtractor(CacheManager cacheManager , ItemEntityRepo repo) {
	     this.cache = cacheManager.getCache(CACHE_NAME) ; 
	     this.repo = repo ; 
	}
	
	
	
	@Override
	public void extract(IPk2Driver driver) {
	 
		log.info("extracting item entities from media.pk2 file");
		List<ItemEntity> list =   driver.findFirst("itemdata.txt")
				.map(jmx -> Pk2ExtractorUtils.toString(jmx, StandardCharsets.UTF_16.name()))
				.map(Pk2ExtractorUtils::toLines)
				.orElseThrow(() -> new Pk2MissedResourceException("Colud not find itemdata.txt file", "itemdata.txt"))
				.flatMap((itemFileName) -> driver.find("(?i)" + itemFileName).stream())
				.flatMap(Pk2ExtractorUtils::toCSVRecordStream)
				.filter((record) -> record.size() > 57 && !record.get(0).startsWith("//"))
				.map(this::toItemEntity)
				.distinct()
				.peek((itemEntity)->cache.put(itemEntity.getLongId(), itemEntity.getRefId()))
				.collect(Collectors.toList()); 
		
		this.repo.saveAll(list) ; 
		
	}
	
	private ItemEntity toItemEntity(CSVRecord record) {
		var builder = ItemEntity.builder();

		String field = record.get(1); // id

		if (NumberUtils.isParsable(field)) {

			builder.refId(Integer.parseInt(field));
		}

		field = record.get(2); // long id

		if (!field.isBlank()) {
			builder.longId(field);
		}

		field = record.get(5); // name
	    String name = 	this.cache.get(field, String.class) ;
	     name = (name == null) ? field : name ; 
	     
		//builder.name(this.cacheStorage.getValueOrDefault(field, String.class, field));
	     builder.name(name); 
	     
		field = record.get(7); // isMallItem {0 , 1}
		boolean isMall = false;
		if (NumberUtils.isParsable(field)) {
			isMall = BooleanUtils.toBoolean(Byte.valueOf(field));
		}

		builder.isMallItem(isMall);

		String type = "";

		field = record.get(10); // type byte 1

		if (NumberUtils.isParsable(field)) {
			type += Integer.toHexString(Integer.parseInt(field));
		}

		field = record.get(11); // type byte 2

		if (NumberUtils.isParsable(field)) {
			type += Integer.toHexString(Integer.parseInt(field));
		}

		field = record.get(12); // type byte 3

		if (NumberUtils.isParsable(field)) {
			type += Integer.toHexString(Integer.parseInt(field));
		}

		builder.itemType(ItemType.parseType(Integer.valueOf(type, 16), record.get(2)));

		field = record.get(14); // item race
		Race itemRace = Race.Universal;

		if (NumberUtils.isParsable(field)) {
			itemRace = Race.parseType(Byte.valueOf(field));
		}
		builder.race(itemRace);

		field = record.get(15); // isSOX

		builder.isSOX(field.equals("1"));

		field = record.get(19);
		builder.isSortable(!field.equals("0"));

		field = record.get(33); // lvl
		int lvl = 0;
		if (NumberUtils.isParsable(field)) {
			lvl = Integer.parseInt(field);
		}
		builder.level(lvl);

		field = record.get(57);
		int maxStacks = 0;
		if (NumberUtils.isParsable(field)) {
			maxStacks = Integer.parseInt(field);
		}

		builder.maxStacks(maxStacks);

		Gender itemGender = Gender.Unisex;

		if (record.size() > 58) {
			field = record.get(58);
			if (NumberUtils.isParsable(field)) {
				itemGender = Gender.parseType(Byte.valueOf(field));
			}
		}

		builder.gender(itemGender);

		int degree = 0;

		if (record.size() > 61) {
			field = record.get(61);
			if (NumberUtils.isParsable(field)) {
				degree = (Integer.parseInt(field) + 3 - 1) / 3;
			}
		}

		builder.degree(degree);

		return builder.build();

	}
}
