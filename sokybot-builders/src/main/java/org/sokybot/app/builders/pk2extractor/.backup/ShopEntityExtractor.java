import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.machinegroup.gamemodel.item.ItemEntity;
import org.sokybot.machinegroup.gamemodel.item.ShopItem;
import org.sokybot.machinegroup.gamemodel.npc.NPCEntity;
import org.sokybot.persistence.entities.NPCType;
import org.sokybot.machinegroup.gamemodel.npc.ShopEntity;
import org.sokybot.machinegroup.gamemodel.npc.ShopTab;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.persistence.service.ItemEntityRepoRepository;
import org.sokybot.persistence.service.NPCEntityRepoRepository;
import org.sokybot.persistence.service.ShopEntityRepoRepository;
import org.sokybot.pk2.IPk2Driver;







import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = IExtractor.class)
@Qualifier("mediapk2")
public class ShopEntityExtractor implements IExtractor {

	private Map<String, Integer> prices = new HashMap<>();
	private Map<String, ItemEntity> packageItems = new HashMap<>();
	
	
	
	private Cache cache ; 
	private ItemEntityRepository itemEntityRepository ; 
	private ShopEntityRepository shopEntityRepository ; 
	private NPCEntityRepository npcEntityRepository ; 
	
	
	@Reference
	public ShopEntityExtractor(CacheManager cacheManger , ItemEntityRepository itemRepository , ShopEntityRepository shopRepository , NPCEntityRepository npcEntityRepository) { 
		this.cache = cacheManger.getCache(CACHE_NAME) ; 
		this.itemEntityRepository = itemRepository ; 
		this.shopEntityRepository = shopRepository ; 
		this.npcEntityRepository = npcEntityRepository ; 
		
	}

	

	@Override
	public void extract(IPk2Driver driver) {

		log.info("extracting Shop entities from media.pk2 file");


		extractPrices(driver);
		extractPackageItems(driver) ; 

		Map<String, List<ShopItem>> tapItems = new Hashtable<>();

		driver.findFirst("refshopgoods.txt").ifPresent((jmx) -> {
			Pk2ExtractorUtils.toCSVRecordStream(jmx).forEach((record) -> {

				String tapLongId = record.get(2);

				List<ShopItem> items = tapItems.get(tapLongId);

				if (items == null) {
					items = new ArrayList<>();
					tapItems.put(tapLongId, items);
				}

				String packId = record.get(3);
				int price = prices.get(packId);
				int itemSlot = Integer.parseInt(record.get(4));
				
				ShopItem item = new ShopItem(this.packageItems.get(packId), price, itemSlot);

				items.add(item);

			});
		});

		Map<Integer, ShopTab> shoptabs = new HashMap<>();

		driver.find("shoptabdata.txt").forEach((jmxFile) -> {
			Pk2ExtractorUtils.toCSVRecordStream(jmxFile).forEach((record) -> {
				String tapId = record.get(1);
				String tapLongId = record.get(2);
				String tapGroupId = record.get(3);
				String shopTapDataName = record.get(4);

				var tapBuilder = ShopTab.builder()
						.refId(Integer.parseInt(tapId))
						.longId(tapLongId)
						.name(shopTapDataName)
						.groupId(Integer.parseInt(tapGroupId));

				if (tapItems.containsKey(tapLongId)) {
					tapItems.get(tapLongId).forEach((item) -> tapBuilder.tabItem(item.getRefId(), item));
				}

				ShopTab tab = tapBuilder.build();
				shoptabs.put(tab.getRefId(), tab);

			});
		});

		driver.findFirst("shopdata.txt").ifPresent((jmx) -> {
		List<ShopEntity> res = 	Pk2ExtractorUtils.toCSVRecordStream(jmx).map((record) -> {

				String field = record.get(1);

				int shopId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;

				field = record.get(5);

				int shopNpcId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
				
				
				NPCEntity entity = this.npcEntityRepository
						.findById(shopNpcId)
						.orElseGet(()->{
							NPCEntity e = NPCEntity.builder().refId(shopNpcId)
									.longId("UNKNOWN" + shopNpcId).Type(NPCType.NPCInteractive).build() ; 
							log.info("NPCEntity with id : {} not exists " , shopNpcId) ; 
							return e ; 
						});
						
				if(entity.getLongId().equals("UNKNOWN"+shopNpcId)) { 
					
					entity = this.npcEntityRepository.save(entity) ; 
					
				}

				ShopTab[] tabs = new ShopTab[10];
				byte tabNumber = 0;
				for (int i = 6; i < 16; i++) {

					field = record.get(i);

					int shopTabId = NumberUtils.isParsable(field) ? Integer.parseInt(field) : -1;
					if (shopTabId != 0) {
						tabs[tabNumber++] = shoptabs.get(shopTabId);
					} else
						break;
				}

				ShopEntity shop = new ShopEntity(shopId, entity, tabs);

				return shop ;
				
			}).collect(Collectors.toList()) ; 
		  ;
		this.shopEntityRepository.saveAll(res) ; 
		});

	}

	
	
	
	private void extractPackageItems(IPk2Driver driver) {
		driver.findFirst("refscrapofpackageitem.txt").ifPresent((jmx) -> {
			Pk2ExtractorUtils.toCSVRecordStream(jmx).filter((r) -> r.size() > 3).forEach((r) -> {
				
				String itemLongId = r.get(3) ; 
				
				ItemEntity itemEntity = this.itemEntityRepository.findItemEntityByLongId(itemLongId)
						.orElse(ItemEntity.builder()
								.longId(itemLongId).build());
				
				this.packageItems.put(r.get(2), itemEntity);
			});

		});

	}




	private void extractPrices(IPk2Driver driver) { 
		driver.findFirst("refpricepolicyofitem.txt").ifPresent((jmx) -> {
			Pk2ExtractorUtils.toCSVRecordStream(jmx).forEach((record) -> {
				prices.put(record.get(2), Integer.parseInt(record.get(5)));
			});

		});		
	}
	
	
	public static void main(String args[]) { 
		
		IPk2Driver driver = IPk2Driver.open("E:\\Amroo\\Silkroad Games\\LegionSRO_15_08_2019\\Media.pk2") ;
		driver.findFirst("refscrapofpackageitem.txt").ifPresent((jmx) -> {
			Pk2ExtractorUtils.toCSVRecordStream(jmx).filter((r) -> r.size() > 3).forEach((r) -> {
				//itemIds.put(r.get(2), this.cacheStorage.getValue(r.get(3), Integer.class));
				System.out.println("Record[2] : " + r.get(2)) ; 
				System.out.println("Record[3] : " + r.get(3)) ; 
				
			});

		});
		
		
	}
}




