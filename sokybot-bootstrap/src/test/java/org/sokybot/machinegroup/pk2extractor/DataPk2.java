package org.sokybot.machinegroup.pk2extractor;

import static org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils.toByteArray;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.sokybot.ICacheStorage;
import org.sokybot.domain.map.ObjectNavMesh;
import org.sokybot.domain.map.ObjectNavMesh.ObjectNavmeshBuilder;
import org.sokybot.machinegroup.pk2extractor.exception.Pk2ExtractionException;
import org.sokybot.machinegroup.service.IDataPk2;
import org.sokybot.domain.map.NavBorder;
import org.sokybot.domain.map.NavCell;
import org.sokybot.domain.map.NavCellLink;
import org.sokybot.domain.map.NavMesh;
import org.sokybot.domain.map.NavObject;
import org.sokybot.domain.map.Position;
import org.sokybot.domain.map.Segment;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataPk2 implements IDataPk2 , IPK2File {

	private final String D_GAME_VERSION = "d_game_version";
	private boolean isReady = false;

	private final String path;

	private final int currentVersion;

	//private final ICacheStorage cacheStorage;

	private final NavMesh navMesh;

	public DataPk2(String dataPath, int currentVersion) {
		this.path = dataPath;
		this.currentVersion = currentVersion;
		

		this.navMesh = new NavMesh(null);

	}

	@Override
	public Optional<ObjectNavMesh> findObjectNavMesh(int objectId) {

		//return Optional.ofNullable(this.cacheStorage.getValue(String.valueOf(objectId), ObjectNavMesh.class));
		return Optional.empty() ;
	}

	

	@Override
	public Optional<Segment> findSegment(short sectorYX) {
		checkState();

		//return Optional.ofNullable(this.cacheStorage.getValue(Integer.toHexString(sectorYX & 0xffff), Segment.class))
			//	.map((seg) -> {
			//		seg.setNavMesh(navMesh);
			//		return seg;
			//	});
		return Optional.empty() ; 
	}


	@Override
	public void refresh() {

		if (registeredVer == -1 || registeredVer != this.currentVersion) {
			try (IPk2Driver driver = IPk2Driver.open(this.path)) {


				extractNavMesh(driver);

				this.cacheStorage.store(D_GAME_VERSION, this.currentVersion);
				this.cacheStorage.flush();

				int storedVersion = this.cacheStorage.getValue(D_GAME_VERSION, Integer.class);
				System.out.println("Stored Version : " + storedVersion);

			} catch (Pk2ExtractionException ex) {
				// because we are working with invalid game so we need to clean our repose to
				// save storage
				this.cacheStorage.destroy();

				throw ex;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

		}

		this.isReady = true;

	}



	
	




		// private Map<Short , NavObject> objLoaded = new HashMap<>() ;
	public static void main(String args[]) throws FileNotFoundException {

		DataPk2 d = new DataPk2(null, 0, null);

		IPk2Driver driver = IPk2Driver
				.open("E:\\Amroo\\Silkroad Games\\RedDiamondSRO_RDSRO_1_042\\RedDiamondSRO_RDSRO_1_042\\Data.pk2");
		long c = System.currentTimeMillis();

		/// driver.find("").forEach((jmx)->{
		// System.out.println(jmx) ;
		// }); ;
		d.extractObjectInfo(driver);

		// d.extractNavMesh(driver);

		c = System.currentTimeMillis() - c;
		System.out.println("Extraction take " + TimeUnit.MILLISECONDS.toSeconds(c) + " second(s)");

		// d.extractNavObjectInfo(IPk2Driver
		// .open("E:\\Amroo\\Silkroad
		// Games\\RedDiamondSRO_RDSRO_1_042\\RedDiamondSRO_RDSRO_1_042\\Data.pk2"));

	}

}
