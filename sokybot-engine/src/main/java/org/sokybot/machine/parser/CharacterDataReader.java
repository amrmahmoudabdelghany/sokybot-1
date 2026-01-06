package org.sokybot.machine.parser;

import org.slf4j.Logger;
import org.sokybot.machinegroup.gamemodel.Gender;
import org.sokybot.machinegroup.gamemodel.Race;
import org.sokybot.machinegroup.gamemodel.item.AbilityPet;
import org.sokybot.machinegroup.gamemodel.item.Equipment;
import org.sokybot.machinegroup.gamemodel.item.Item;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.machinegroup.gamemodel.item.ItemExchangeCoupon;
import org.sokybot.machinegroup.gamemodel.item.ItemRent;
import org.sokybot.machinegroup.gamemodel.item.ItemType;
import org.sokybot.machinegroup.gamemodel.item.MagParamType;
import org.sokybot.machinegroup.gamemodel.item.MagicCube;
import org.sokybot.machinegroup.gamemodel.item.PetScroll;
import org.sokybot.machinegroup.gamemodel.item.PetStatus;
import org.sokybot.machinegroup.gamemodel.npc.HKSlotType;
import org.sokybot.machinegroup.gamemodel.npc.HotKey;
import org.sokybot.machinegroup.gamemodel.quest.Objective;
import org.sokybot.machinegroup.gamemodel.quest.ObjectiveStatus;
import org.sokybot.machinegroup.gamemodel.quest.Quest;
import org.sokybot.machinegroup.gamemodel.quest.QuestStatus;
import org.sokybot.machinegroup.gamemodel.skill.Buff;
import org.sokybot.machinegroup.gamemodel.skill.Mastery;
import org.sokybot.machinegroup.gamemodel.skill.Skill;
import org.sokybot.persistence.entities.SkillEntity;
import org.sokybot.machinegroup.gamemodel.skill.SkillType;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.sokybot.network.packet.IStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.experimental.Delegate;

@Component
@Scope("prototype")
public class CharacterDataReader implements ICharacterDataReader {

	@Delegate
	private IStreamReader reader;

	@Autowired
	private ISroMaterialDAO gameDao;
	
	@Autowired
	private Logger log ; 

	public CharacterDataReader(IStreamReader reader) {
		this.reader = reader;
	}

	@Override
	public Item getItem() {

		byte itemSlot = reader.getByte();
		int rentType = reader.getInt();
		
		ItemRent rent = new ItemRent();

		switch (rentType) {
		case 1:
			rent.setCanDelete(reader.getShort());
			rent.setPeriodBeginTime(reader.getInt());
			rent.setPeriodEndTime(reader.getInt());
			break;
		case 2:
			rent.setCanDelete(reader.getShort());
			rent.setCanRecharge(reader.getShort());
			rent.setMeterRateTime(reader.getInt());

			break;
		case 3:
			rent.setCanDelete(reader.getShort());
			rent.setPeriodBeginTime(reader.getInt());
			rent.setPeriodEndTime(reader.getInt());
			rent.setCanRecharge(reader.getShort());
			rent.setPackingTime(reader.getInt());

			break;

		}

		int refId = reader.getInt();

		
		return this.gameDao.findItemEntity(refId).map((itemEntity) -> {
			
			Item item = new Item(itemEntity);
			item.setSlot(itemSlot);
			item.setRentType(rentType);
			item.setRent(rent);

			if (item.isEquipmentItem()) {
				Equipment equipment = new Equipment(item);
				equipment.setOptLvl(reader.getByte());
				equipment.setAttributeValue(reader.getLong());
				equipment.setDurability(reader.getInt());

				byte magParamNumber = reader.getByte();

				equipment.setMagParamNum(magParamNumber);
				for (int i = 0; i < magParamNumber; i++) {
					MagParamType type = MagParamType.of(reader.getInt());
					equipment.setMagParamValue(type, reader.getInt());

				}

				byte optType = reader.getByte(); // (1 => Socket)
				byte optCount = reader.getByte();

				for (int i = 0; i < optCount; i++) {
					equipment.setOptSlot(reader.getByte());
					equipment.setOptID(reader.getInt());
					equipment.setOptNParam1(reader.getInt());
				}

				optType = reader.getByte();
				optCount = reader.getByte();

				for (int i = 0; i < optCount; i++) {
					equipment.setOptSlot(reader.getByte());
					equipment.setOptID(reader.getInt());
					equipment.setOptValue(reader.getInt());
				}

				return equipment;

			} else if (item.isMagicStone() || item.isAttributeStone()
					|| item.getItemType() == ItemType.MagicStoneMall) {

				item.setStackCount(reader.getShort());

				if (item.getItemType() != ItemType.MagicStoneMall && item.getItemType() != ItemType.MagicStoneSteady
						&& item.getItemType() != ItemType.MagicStoneLuck) {
					item.setAttributeAssimilationProbability(reader.getByte());
				}

				return item;

			} else if (item.getItemType() == ItemType.GrowthPet) {

				PetScroll pet = new PetScroll(item);
				PetStatus petStatus = PetStatus.of(reader.getByte());
				pet.setPetStatus(petStatus);

				if (petStatus != PetStatus.Unsummoned) {

					pet.setUniqueId(reader.getInt());
					pet.setPetName(new String(reader.getBytes(reader.getShort())));
					pet.setUnk02(reader.getByte());

				}

				return pet;

			} else if (item.getItemType() == ItemType.AbilityPet) {

				AbilityPet abilityPet = new AbilityPet(item);
				PetStatus petStatus = PetStatus.of(reader.getByte());
				abilityPet.setPetStatus(petStatus);

				if (petStatus != PetStatus.Unsummoned) {

					abilityPet.setUniqueId(reader.getInt());
					abilityPet.setPetName(new String(reader.getBytes(reader.getShort())));
					abilityPet.setSecondsToRentEndTime(reader.getInt());
					abilityPet.setUnk02(reader.getByte());

				}
				return abilityPet;

			} else if (item.getItemType() == ItemType.ItemExchangeCoupon) {

				ItemExchangeCoupon iec = new ItemExchangeCoupon(item);

				iec.setStackCount(reader.getShort());

				byte magParamNum = reader.getByte();

				for (int i = 0; i < magParamNum; i++) {

					iec.addMagParamValue(reader.getLong());
				}

				return iec;
			} else if (item.getItemType() == ItemType.MagicCube) {

				MagicCube cube = new MagicCube(item);

				cube.setStoredItemCount(reader.getInt());

				return cube;

			} else {
				item.setStackCount(reader.getShort());
				return item;
			}

		}).orElseGet(() -> {
			
			ItemEntity itemEntity = ItemEntity.builder()
					.refId(refId)
					.itemType(ItemType.UNKNOWN)
					.gender(Gender.Unisex)
					.race(Race.Universal)
					.build();

			Item item = new Item(itemEntity);
			item.setRentType(rentType);
			item.setRent(rent);
			item.setSlot(itemSlot);

			return item;
		});

	}

	@Override
	public Quest getQuest() {
		Quest q = new Quest(reader.getInt()) ; 
		q.setAchievementCount(reader.getByte());// (Repetition Amount = Bit && Completetion Amount = Bit)
		q.setRequiresSharePt(reader.getByte());
		q.setType(reader.getByte());
		
		if(q.getType() == 28) { 
			q.setRemainingTime(reader.getInt());
		}
		
		q.setStatus(QuestStatus.of(reader.getByte()));
		
		if(q.getType() != 8) { 
			
			byte objectiveCount = reader.getByte() ; 
			
			for(int x = 0 ; x < objectiveCount ; x++) { 
				Objective objective = new Objective(reader.getByte()) ;
				objective.setStatus(ObjectiveStatus.of(reader.getByte())) ; 
				objective.setName(reader.getString());
				
				byte taskCount = reader.getByte() ; 
				
				for(int xx = 0 ; xx < taskCount ; xx++) { 
					objective.addTask(reader.getInt());
				}

				q.addQuestObjective(objective);
			}
			
			
		}
		
		if(q.getType() == 88) { 
			
			byte taskCount = reader.getByte() ; 
			
			for(int x = 0 ; x < taskCount ; x++) { 
				
				q.addTaskRefObjId(reader.getInt());
				
			}
			
		}

		return q ; 
	}

	@Override
	public Skill getSkill() {
		int refId = reader.getInt();

		Skill skill = this.gameDao.findSkillEntity(refId).map(Skill::new).orElseGet(() -> {
			return new Skill(SkillEntity.builder().type(SkillType.UNKNOWN).refId(refId).build());
		});

		skill.setIsEnabled(reader.getByte());

		return skill;
	}

	@Override
	public Buff getBuff() {
		int bufRefId = reader.getInt();

		Buff buff=  this.gameDao.findSkillEntity(bufRefId).map((entity) -> {
			Buff res = new Buff(entity);
			res.setBuffDuration(reader.getInt());
			log.info("Readed Buf {} " , res); 
			return res;
		}).orElseGet(() -> {
			log.info("Could not find Buff with refId {} " , bufRefId) ; 
			return new Buff(SkillEntity.builder().type(SkillType.UNKNOWN).refId(bufRefId).build());

		});
		
		if(buff.isTransferableBuff()) { 
			buff.setCreator(reader.getBoolean());
		}

		return buff ; 
	}

	@Override
	public Mastery getMastery() {
		Mastery mastery = new Mastery();
		int id = reader.getInt() ; 
		mastery.setMasteryID(id);
		mastery.setMasteryLevel(reader.getByte());
		mastery.setName(this.gameDao.findMasteryName(id).orElse("Unknown")) ; 
		System.out.println("Mastery id : " + mastery.getMasteryID()  +
				" , name  : " + mastery.getName()
				+ " , Level : " + mastery.getMasteryLevel()) ; 
		
		return mastery;
	}

	@Override
	public HotKey getHotKey() {
		HotKey hk = new HotKey() ; 
		hk.setSlotSeq(reader.getByte());
		hk.setSlotType(HKSlotType.of(reader.getByte()));
		hk.setData(reader.getInt());
		return hk ; 
	}

	
}
