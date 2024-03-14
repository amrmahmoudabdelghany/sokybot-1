package org.sokybot.machinegroup.pk2extractor.mediapk2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.machinegroup.gamemodel.LvlEXP;
import org.sokybot.machinegroup.pk2extractor.IExtractor;
import org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.machinegroup.repo.LvlDataRepo;
import org.sokybot.pk2.IPk2Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
@Qualifier("mediapk2")
public class LvlDataExtractor implements IExtractor {

	@Autowired
	private LvlDataRepo dataRepo;

	@Override
	public void extract(IPk2Driver driver) {

		log.info("extracting lvl data");
		driver.findFirst("leveldata.txt").ifPresent((jmx) -> {
			List<LvlEXP> res = Pk2ExtractorUtils.toCSVRecordStream(jmx , StandardCharsets.UTF_16).map((record) -> {
				String field = record.get(1);
				Long exp = NumberUtils.isParsable(field) ? Long.parseLong(field) : Long.MAX_VALUE;
				// this.cacheStorage.store("LVL_" + record.get(0) + "_EXP", exp);
				field = record.get(0);

				return new LvlEXP(Integer.parseInt(field), exp);

			}).collect(Collectors.toList()) ; 
			  
			this.dataRepo.saveAll(res);
		});
	}


}
