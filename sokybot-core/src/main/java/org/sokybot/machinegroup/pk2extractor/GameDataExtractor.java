package org.sokybot.machinegroup.pk2extractor;

import java.io.IOException;

import org.sokybot.app.AppConstants;
import org.sokybot.machinegroup.gamemodel.GameInfo;
import org.sokybot.machinegroup.pk2extractor.exception.Pk2ExtractionException;
import org.sokybot.machinegroup.pk2extractor.exception.Pk2MissedResourceException;
import org.sokybot.machinegroup.repo.GameInfoRepo;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.security.Blowfish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GameDataExtractor implements CommandLineRunner {

	@Autowired
	private ApplicationContext ctx;

	//@Value("${" + AppConstants.GAME_PATH + "}")
	@Value("${gamePath}")
	private String gamePath;

	
	
	
	
	
	@Override
	public void run(String... args) throws Exception {

		int currentVersion = extractVersion();
		
		int registeredVersion = this.ctx.getBean(GameInfoRepo.class)
				.findById(this.gamePath)
				.map(GameInfo::getVersion)
				.orElse(-1);

		if (registeredVersion == -1 || registeredVersion != currentVersion) {
			log.info("Game {} Registered Version {} , Actual version {} ", this.gamePath, registeredVersion,
					currentVersion);
			try (IPk2Driver driver = IPk2Driver.open(this.gamePath + "\\Media.pk2")) {

				this.ctx.getBean("mediaPk2Extractor", IExtractor.class).extract(driver);
				
			
			}catch (Pk2ExtractionException ex) {
				// TODO  we need to handle this exception
			}
			try (IPk2Driver driver = IPk2Driver.open(this.gamePath + "\\Data.pk2")) {

				this.ctx.getBean("dataPk2Extractor" , IExtractor.class).extract(driver);;
					
			
			}catch (Pk2ExtractionException ex) {
				// TODO  we need to handle this exception
			}
						
			//TODO clear cache
		}
		

	}
	
	
	
	
	

	private int extractVersion() {
		try (IPk2Driver driver = IPk2Driver.open(this.gamePath + "\\Media.pk2")) {
			int version = driver.findFirst("SV.T")
					.map(Pk2ExtractorUtils::firstChunk)
					.map((bytes) -> Blowfish.newInstance("SILKROAD".getBytes()).decode(0, bytes))
					.map(String::new)
					.map(String::trim)
					.map(Pk2ExtractorUtils::toInteger)
					.orElseThrow(() -> new Pk2MissedResourceException("Colud not find SV.T file ", "SV.T"));
			return version;
		} catch (IOException e) {

			e.printStackTrace();
		}
		return -1;
	}

}
