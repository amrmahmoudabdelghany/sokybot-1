package org.sokybot.machinegroup.pk2extractor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sokybot.ICacheStorage;
import org.sokybot.machinegroup.pk2extractor.exception.Pk2MissedResourceException;
import org.sokybot.machinegroup.service.IDataPk2;
import org.sokybot.machinegroup.service.IMediaPk2;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.security.Blowfish;

public class Pk2Extractors implements IEntityExtractorFactory{

	private final Map<String, IMediaPk2> pk2FileCache  = new ConcurrentHashMap<>(); 
	
	private final String gamePath ; 
	
	private final int currentVersion ; 
	
	
	
	
	public Pk2Extractors(String gamePath ) { 
		this.gamePath = gamePath ; 
		
		
		// extract current game version and save it to cache storage 
				// so 
		int extractedVersion = -1 ; 
		
		try(IPk2Driver mediaPk2 = IPk2Driver.open(gamePath + "\\Media.pk2")) { 
			
			extractedVersion = extractVersion(mediaPk2) ; 
			
		}catch (IOException e) {
			e.printStackTrace();
			
			extractedVersion = -1 ; 
		}finally {
			this.currentVersion = extractedVersion ; 
		}
	}
	
	
	private int extractVersion(IPk2Driver mediaPk2) {
		
		return mediaPk2.findFirst("SV.T")
				.map(Pk2ExtractorUtils::firstChunk)
				.map((bytes) -> Blowfish.newInstance("SILKROAD".getBytes()).decode(0, bytes))
				.map(String::new)
				.map(String::trim)
				.map(Pk2ExtractorUtils::toInteger)
				.orElseThrow(() -> new Pk2MissedResourceException("Colud not find SV.T file ", "SV.T"));

	}
	
	@Override
	public IDataPk2 getDataPk2() {
	
		return  new DataPk2(gamePath + "\\Data.pk2" , this.currentVersion) ;
	}
	
	@Override
	public  IMediaPk2 getMediaPk2() { 
		
		IMediaPk2 mediaPk2 = this.pk2FileCache.get(gamePath) ; 
		
		if(mediaPk2 == null) { 
			
			//mediaPk2 = new MediaPk2(gamePath + "\\Media.pk2" , this.currentVersion ,
				//						this.cacheStorage  );
			pk2FileCache.put(gamePath, mediaPk2) ; 
		}
		
	
		return mediaPk2 ; 
	}
}
