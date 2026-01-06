import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.mediapk2;

import static org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils.toByteArray;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.sokybot.app.AppConstants;
import org.sokybot.persistence.entities.Division;
import org.sokybot.persistence.entities.DivisionInfo;
import org.sokybot.persistence.entities.GameInfo;
import org.sokybot.persistence.entities.SilkroadType;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.app.builders.pk2extractor.exception.Pk2InvalidResourceFormatException;
import org.sokybot.app.builders.pk2extractor.exception.Pk2MissedResourceException;
import org.sokybot.persistence.service.GameInfoRepoRepository;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.security.Blowfish;







import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component(service = IExtractor.class)
public class GameInfoExtractor implements IExtractor{

	
	private String gamePath ; 
	
	private GameInfoRepository gameInfoRepository ; 
	
	@Reference
	public GameInfoExtractor(@Value("${"+ AppConstants.GAME_PATH + "}")  String gamePath  , GameInfoRepository repo) { 
		this.gamePath = gamePath ; 
		this.gameInfoRepository = repo ; 
	}
	
	
	@Override
	public void extract(IPk2Driver driver) {
	 

		log.info("extracting game information from media.pk2 file ");
	GameInfo gameInfo = GameInfo.builder()
			.gamePath(gamePath)
			.divisionInfo(extractDivisionInfo(driver))
			.silkroadType(extractSilkroadType(driver))
			.port(extractPort(driver))
			.version(extractVersion(driver)).build() ; 
	
	
	this.gameInfoRepository.save(gameInfo) ; 

	}
	
	
	private DivisionInfo extractDivisionInfo(IPk2Driver driver) { 
		log.info("extracting division information from media.pk2 file ");
		 return  driver.findFirst("(?i)divisioninfo.txt").map((jmxFile) -> {
			DivisionInfo divInfo = new DivisionInfo();
			ByteBuffer buffer = ByteBuffer.wrap(toByteArray(jmxFile)).order(ByteOrder.LITTLE_ENDIAN);
			divInfo.local = nextByte(buffer);
			byte divCount = nextByte(buffer);

			Stream.generate(Division::new).limit(divCount).forEach((div) -> {
				div.setName(nextString(buffer));
				nextByte(buffer); // skip 1 byte
				byte ipCount = nextByte(buffer);
				for (int i = 0; i < ipCount; i++) {
					div.addHost(nextString(buffer));
					nextByte(buffer); // skip 1 byte
				}
				divInfo.addDivision(div);
			});

			return divInfo;

		}).orElseThrow(() -> new Pk2MissedResourceException("Could not find Media.pk2$divisioninfo.txt file  ",
						"divisioninfo.txt"));
		
	}
	
	private String nextString(ByteBuffer buffer) {

		try {
			byte[] strBuffer = new byte[buffer.getInt()];
			buffer.get(strBuffer);
			return new String(strBuffer);
		} catch (IndexOutOfBoundsException | BufferUnderflowException ex) {
			throw new Pk2InvalidResourceFormatException("An error occurred while parsing a joymax resource file ", ex);
		}

	}
	private byte nextByte(ByteBuffer buffer) {
		try {
			return buffer.get();
		} catch (BufferUnderflowException ex) {
			throw new Pk2InvalidResourceFormatException("An error occurred while parsing a joymax resource file ", ex);
		}
	}

	private int extractVersion(IPk2Driver driver) { 
		log.info("extracting game version from media.pk2 file ");
		return driver.findFirst("SV.T")
				.map(Pk2ExtractorUtils::firstChunk)
				.map((bytes) -> Blowfish.newInstance("SILKROAD".getBytes()).decode(0, bytes))
				.map(String::new)
				.map(String::trim)
				.map(Pk2ExtractorUtils::toInteger)
				.orElseThrow(() -> new Pk2MissedResourceException("Colud not find SV.T file ", "SV.T"));
				
	}
	private int extractPort(IPk2Driver driver) { 
		log.info("extracting port from media.pk2 file");
		return  driver.findFirst("(?i)gateport.txt")
				.map(Pk2ExtractorUtils::toText)
				.map(String::trim)
				.map(Pk2ExtractorUtils::toInteger)
				.orElseThrow(() -> new Pk2MissedResourceException("Could not find gateport.txt file ", "gateport.txt"));

	}
	
	private SilkroadType extractSilkroadType(IPk2Driver driver) { 
		log.info("extracting game type info from media.pk2 file");
		return driver.findFirst("type.txt")
				.map(Pk2ExtractorUtils::toText)
				.map(Pk2ExtractorUtils::toLines)
				.map((lines) -> {

					Map<String, String> props = new HashMap<>();
					AtomicBoolean isEmpty = new AtomicBoolean(true);
					lines.filter(line -> line.contains("="))

							.forEach((line) -> {
								String parts[] = StringUtils.split(line, "=");
								props.put(parts[0].trim(), parts[1].replace("\"", " ").trim());
								isEmpty.set(false);
							});

					if (isEmpty.get()) {
						throw new Pk2InvalidResourceFormatException("Unexpected format for joymax file type.txt",
								"type.txt");
					}

					return new SilkroadType(props);
				})
				.orElseThrow(() -> new Pk2MissedResourceException("Could not find joymax file  type.txt", "type.txt"));

	}
}




