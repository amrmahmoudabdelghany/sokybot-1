package org.sokybot.machinegroup.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import org.sokybot.app.AppConstants;
import org.sokybot.machinegroup.gamemodel.DivisionInfo;
import org.sokybot.machinegroup.gamemodel.GameInfo;
import org.sokybot.machinegroup.gamemodel.LvlEXP;
import org.sokybot.machinegroup.gamemodel.MasteryData;
import org.sokybot.machinegroup.gamemodel.SilkroadEntity;
import org.sokybot.machinegroup.gamemodel.SilkroadType;
import org.sokybot.machinegroup.gamemodel.geo.Vector2D;
import org.sokybot.machinegroup.gamemodel.item.ItemEntity;
import org.sokybot.machinegroup.gamemodel.navmesh.ObjectNavMesh;
import org.sokybot.machinegroup.gamemodel.navmesh.Position;
import org.sokybot.machinegroup.gamemodel.navmesh.SectorRef;
import org.sokybot.machinegroup.gamemodel.npc.NPCEntity;
import org.sokybot.machinegroup.gamemodel.npc.ShopEntity;
import org.sokybot.machinegroup.gamemodel.portal.PortalEntity;
import org.sokybot.machinegroup.gamemodel.portal.TeleportEntity;
import org.sokybot.machinegroup.gamemodel.skill.SkillEntity;
import org.sokybot.machinegroup.mapnavigation.RuteFinder;
import org.sokybot.machinegroup.mapnavigation.Sector;
import org.sokybot.machinegroup.pk2extractor.IEntityExtractorFactory;
import org.sokybot.machinegroup.pk2extractor.IPK2File;
import org.sokybot.machinegroup.repo.GameInfoRepo;
import org.sokybot.machinegroup.repo.ItemEntityRepo;
import org.sokybot.machinegroup.repo.LvlDataRepo;
import org.sokybot.machinegroup.repo.MasteryDataRepo;
import org.sokybot.machinegroup.repo.NPCEntityRepo;
import org.sokybot.machinegroup.repo.ObjectNavMeshRepo;
import org.sokybot.machinegroup.repo.PortalEntityRepo;
import org.sokybot.machinegroup.repo.SegmentRepo;
import org.sokybot.machinegroup.repo.ShopEntityRepo;
import org.sokybot.machinegroup.repo.SkillEntityRepo;
import org.sokybot.machinegroup.repo.TeleportEntityRepo;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.utils.DDSReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class SroMaterialDAO implements ISroMaterialDAO {

	@Autowired
	ResourceLoader resourceLoader;

	@Autowired
	private NPCEntityRepo npcEntityRepo;

	@Autowired
	private SkillEntityRepo skillEntityRepo;

	@Autowired
	private ItemEntityRepo itemEntityRepo;

	@Autowired
	private TeleportEntityRepo teleportEntityRepo;

	@Autowired
	private ShopEntityRepo shopEntityRepo;

	@Autowired
	private PortalEntityRepo portalEntityRepo;

	@Autowired
	private GameInfoRepo gameInfoRepo;

	@Autowired
	private MasteryDataRepo masteryInfoRepo;

	@Autowired
	private LvlDataRepo lvlDataRepo;

	@Autowired
	private ObjectNavMeshRepo objectNavMeshRepo ;
	
	@Autowired
	private SegmentRepo segmentRepo;

	@Autowired
	private Executor executor;
	
	private Map<String, Image> tmp = new HashMap() ; 


	
	
	private Map<Short , SectorRef> tempCache = new HashMap<>() ; 
	
	@Value("${gamePath}")
	private String gamePath;

	private  SectorRefExtractor sectorRefExtractor ;

	private Optional<GameInfo> gameInfo = Optional.empty();
	private RuteFinder aStar = new RuteFinder(this) ; 
	

	
	
	@PostConstruct
	private void init() { 
		   this.sectorRefExtractor =  new SectorRefExtractor(gamePath); ; 
				
	}
	@Override
	public String getGamePath() {
		return this.gamePath;
	}

	@Override
	public List<Vector2D> findPath(int x1, int y1, int x2, int y2) {
		
		return findPath(new Position(x1, 0, y1) , new Position(x2, 0, y2)) ; 
		
	}
	@Override
	public List<Vector2D> findPath(Position start, Position end) {


		List<Vector2D> path = this.aStar.findPath(start, end, 1) ;
		Collections.reverse(path) ; 
		return path ; 
	}

	@Override
	public Optional<Image> findCharacterIcon(int charId) {

		Image image;
		try {
			image = ImageIO.read(this.resourceLoader
					.getResource("classpath:icons/char-icon/0x" + Integer.toHexString(charId) + ".png")
					.getURL());
		} catch (IOException e) {

			image = null;
		}

		return Optional.ofNullable(image);
	}

	@Override
	public Optional<Byte> getLocal() {
		return this.findDivisionInfo().map((div) -> div.local);
	}

	@Override
	public int getPort() {
		System.out.println("On Get Port : getGameInfo() was null :  " + getGameInfo() == null  ); 
		
		return getGameInfo().map(GameInfo::getPort).orElse(-1);
	}

	@Override
	public Optional<String> getRndHost() {
		return this.findDivisionInfo().map((div) -> div.getDivisions().get(0).getRandomHost());

	}

	@Override
	public Optional<SilkroadType> findType() {
		return getGameInfo().map(GameInfo::getSilkroadType);
	}

	@Override
	public Optional<DivisionInfo> findDivisionInfo() {
		return getGameInfo().map(GameInfo::getDivisionInfo);
	}

	private Optional<GameInfo> getGameInfo() {
		if (this.gameInfo.isEmpty()) {
			this.gameInfo = this.gameInfoRepo.findById(this.gamePath);
		}
		return this.gameInfo;
	}

	@Override
	public int getVersion() {
		
		return getGameInfo().map(GameInfo::getVersion).orElse(-1);
	}

	@Override
	public Optional<SkillEntity> findSkillEntity(int refId) {
		return this.skillEntityRepo.findById(refId);
	}

	@Override
	public Optional<ItemEntity> findItemEntity(int refId) {

		return this.itemEntityRepo.findById(refId);
	}

	@Override
	public Optional<ShopEntity> findShop(int npcRefId) {
		return this.shopEntityRepo.findById(npcRefId);
	}

	@Override
	public Optional<Long> getLvlEXP(int lvl) {
		return this.lvlDataRepo.findById(lvl).map(LvlEXP::getExp);
	}

	@Override
	public Optional<TeleportEntity> findTeleport(int refId) {
		return this.teleportEntityRepo.findById(refId);
	}

	@Override
	public Optional<PortalEntity> findPortal(int refId) {
		return this.portalEntityRepo.findById(refId);
	}

	@Override
	public Optional<NPCEntity> findNPC(int refId) {
		return this.npcEntityRepo.findById(refId);
	}
//
//	@Override
//	public Optional<SilkroadEntity> findEntity(int refId) {
//		
//		if(this.itemEntityRepo.existsById(refId)) { 
//			return this.itemEntityRepo.findById(refId).map((item)->(SilkroadEntity)item); 
//		}
//		
//		
//		
//		
//		return Optional.empty();
//	}

	@Override
	public Optional<String> findMasteryName(int masteryId) {
		return this.masteryInfoRepo.findById(masteryId).map(MasteryData::getName);
	}


	
	public Optional<SectorRef> findSegment(byte sectorX, byte sectorY) {

		return findSegment((short) (((sectorY & 0xFF) << 8) | (sectorX & 0xFF)));
	}
	
	
	
	public List<SectorRef> findAllSectors() { 
		return this.sectorRefExtractor.extractAllSegments() ; 
	}
	public Optional<SectorRef> findSegment(short sectorYX) {

		
		if(tempCache.containsKey(sectorYX)) { 
			return Optional.ofNullable(tempCache.get(sectorYX)) ; 
		}else { 
			Optional<SectorRef> seg = this.sectorRefExtractor.extractSegment(Integer.toHexString(sectorYX & 0xffff));
			seg.ifPresent((s)->{
				this.tempCache.put(sectorYX, s) ;
			});
			return seg ; 
		}
		
		/*
		if (this.segmentRepo.existsById(sectorYX)) {
			return this.segmentRepo.findById(sectorYX);
		} else {
			Optional<Segment> seg = ops.extractSegment(Integer.toHexString(sectorYX & 0xffff));

			seg.ifPresent((s) -> {
			//	this.executor.execute(() -> this.segmentRepo.save(s));
			});

			return seg;
		}
*/
	}

	public Optional<Image> findSectorMinimap(short x, short y) {
		   String id = String.valueOf(x) + "x" + String.valueOf(y) ; 
	
		  Image res =  null ; 
		  if(tmp.containsKey(id)) { 
			  res =  tmp.get(id);
 		  }else { 
 			  Image image = extractMiniMap(x, y) ; 
 			  tmp.put(id, image)  ; 
 			  res = image ; 
 		  }
		return Optional.of(res);
	}



	private Image extractMiniMap(short sectorX, short sectorY) {
		try (IPk2Driver driver = IPk2Driver.open(this.gamePath + "\\Media.pk2")) {
			String name = String.valueOf(sectorX) + "x" + String.valueOf(sectorY) + ".ddj";

			return driver.findFirst("minimap\\" + name).map((jmx) -> {

				try {
					InputStream stream = jmx.getInputStream();
					stream.skip(20);
					byte [] arr = IOUtils.toByteArray(stream) ; 
					
				    int [] pixels = DDSReader.read(arr, DDSReader.ARGB, 0) ; 
					int width = DDSReader.getWidth(arr) ; 
					int height = DDSReader.getHeight(arr) ; 
					BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) ; 
					image.setRGB(0, 0, width, height, pixels, 0	, width) ; 
					return (Image) image ; 
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}).orElse((Image) new BufferedImage(192, 192, BufferedImage.TYPE_4BYTE_ABGR));

		} catch (IOException e1) {
			throw new UncheckedIOException(e1) ; 
		}
	}

	
	@Override
	public Optional<ObjectNavMesh> findObjectNavMesh(int objectId) {
		return this.objectNavMeshRepo.findById(objectId) ; 
	}

	@Override
	public Map<String, List<String>> getDivHosts() {
		Map<String, List<String>> res = new HashMap<>();

		getGameInfo().map(GameInfo::getDivisionInfo).map(DivisionInfo::getDivisions).ifPresent((divs) -> {
			divs.stream().forEach((div) -> {
				res.put(div.getName(), div.getHosts());
			});
		});

		return res;
	}

	@Override
	public Optional<String> getLanguage() {
		return getGameInfo().map(GameInfo::getSilkroadType).map(SilkroadType::getLanguage);

	}

	@Override
	public Optional<String> getCountry() {
		return getGameInfo().map(GameInfo::getSilkroadType).map(SilkroadType::getCountry);
	}

	@Override
	public List<NPCEntity> findAllMonsterLike(String filter) {
		return this.npcEntityRepo.findAllMonsterLike(filter);
	}

}
