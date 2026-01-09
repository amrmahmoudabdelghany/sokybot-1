package org.sokybot.machine.parser;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.sokybot.machinegroup.gamemodel.ISpawnable;
import org.sokybot.machinegroup.gamemodel.item.DropItem;
import org.sokybot.machinegroup.gamemodel.item.Equipment;
import org.sokybot.machinegroup.gamemodel.item.Item;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.machinegroup.gamemodel.item.Rarity;
import org.sokybot.machinegroup.gamemodel.npc.CharacterStatus;
import org.sokybot.machinegroup.gamemodel.npc.DebuffStatus;
import org.sokybot.machinegroup.gamemodel.npc.IFighter;
import org.sokybot.machinegroup.gamemodel.npc.InteractionMode;
import org.sokybot.machinegroup.gamemodel.npc.JobType;
import org.sokybot.machinegroup.gamemodel.npc.LifeState;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.machinegroup.gamemodel.npc.MotionState;
import org.sokybot.machinegroup.gamemodel.npc.MovementType;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.machinegroup.gamemodel.npc.NPCType;
import org.sokybot.machinegroup.gamemodel.npc.PVPState;
import org.sokybot.machinegroup.gamemodel.npc.Pet;
import org.sokybot.machinegroup.gamemodel.npc.Player;
import org.sokybot.machinegroup.gamemodel.portal.Portal;
import org.sokybot.persistence.entities.PortalEntity;
import org.sokybot.machinegroup.gamemodel.skill.Buff;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.utils.SilkroadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.experimental.Delegate;

@Component
@Scope("prototype")
public class SpawnParser implements ISpawnParser {

	@Delegate
	private IStreamReader reader;

	@Autowired
	private IGameDataLookup sroDAO;
	
	@Autowired
	private Logger log ; 

	public SpawnParser(IStreamReader reader) {
		this.reader = reader;
	}

	private void readSpawnData(ISpawnable spawnable) {
				
		spawnable.setUniqueId(reader.getInt());

		spawnable.setXSector(reader.getUnsignedByte());
		spawnable.setYSector(reader.getUnsignedByte());
		
		spawnable.setXOffset(reader.getFloat());
		spawnable.setZOffset(reader.getFloat());
		spawnable.setYOffset(reader.getFloat());
		spawnable.setAngle(SilkroadUtils.getAngle(reader.getShort()));
		

		spawnable.setLocation(
				SilkroadUtils.getXCoord(spawnable.getXOffset(), spawnable.getXSector(), 10),
				SilkroadUtils.getYCoord(spawnable.getYOffset(), spawnable.getYSector(), 10));
		

	//	log.info("Spawnable Location ({} , {})  " , spawnable.getX() , spawnable.getY());
		
		
		
	}

	private void readFighterData(IFighter fighter) {
		fighter.setHasDestination(reader.getBoolean());
		fighter.setMovementType(MovementType.of(reader.getByte()));

		if (fighter.isHasDestination()) {

			fighter.setDestXSector(reader.getUnsignedByte());
			fighter.setDestYSector(reader.getUnsignedByte());

			if (fighter.getDestYSector() == 0x80) {

				fighter.setDestXOffset(reader.getShort() - reader.getShort());
				fighter.setDestZOffset(reader.getShort() - reader.getShort());
				fighter.setDestYOffset(reader.getShort() - reader.getShort());

			} else {

				fighter.setDestXOffset(reader.getShort());
				fighter.setDestZOffset(reader.getShort());
				fighter.setDestYOffset(reader.getShort());

			}

			fighter.setDestX(SilkroadUtils.getXCoord(fighter.getDestXOffset(), fighter.getDestXSector())) ; 
			fighter.setDestY(SilkroadUtils.getYCoord(fighter.getDestYOffset(), fighter.getDestYSector()));

		} else {

			fighter.setSkyClickFlag(reader.getByte());
			fighter.setAngle(SilkroadUtils.getAngle(reader.getShort()));

		}
		
		

		fighter.setLifeState(LifeState.of(reader.getByte()));
		fighter.setDebuffStatus(DebuffStatus.of(reader.getByte()));
		fighter.setMotionState(MotionState.of(reader.getByte()));
		fighter.setCharacterStatus(CharacterStatus.of(reader.getByte()));

		fighter.setWalkSpeed(reader.getFloat());
		fighter.setRunSpeed(reader.getFloat());
		fighter.setHwanSpeed(reader.getFloat());
		;
		byte activeBuffCount = reader.getByte();

		for (int i = 0; i < activeBuffCount; i++) {
			this.sroDAO.findSkill(reader.getInt()).ifPresent((skill) -> {
				Buff buff = new Buff(skill) ; 
				buff.setBuffDuration(reader.getInt()) ; 
				
				if(buff.isTransferableBuff()) { 
					buff.setCreator(reader.getBoolean());
				}
				fighter.addBuff(buff);
				
			});
		}

	}

	@Override
	public DropItem readDropItem(ItemEntity entity) {

		DropItem dropItem = new DropItem(entity);

		String longId = dropItem.getLongId();

		if (longId.startsWith("ITEM_ETC_GOLD")) {
			dropItem.setAmount(reader.getInt());
		}

		if (longId.startsWith("ITEM_QSP") || longId.startsWith("ITEM_ETC_E090825") || longId.startsWith("ITEM_QNO")
				|| longId.startsWith("ITEM_TRADE_SPECIAL_BOX")) {

			dropItem.setName(reader.getString());
		}

		if (longId.startsWith("ITEM_CH") || longId.startsWith("ITEM_EU")) {
			dropItem.setPlus(reader.getByte());

		}

		readSpawnData(dropItem);

		byte hasOwner = reader.getByte();

		if (hasOwner == 1) {

			dropItem.setOwnerId(reader.getInt());

		}

		dropItem.setRarity(Rarity.of(reader.getByte()));

		return dropItem;
	}

	
	@Override
	public Monster readMonster(NPCEntity entity) {
	
		Monster monster = new Monster(entity) ; 
		
		readSpawnData(monster);
		readFighterData(monster);
		
		reader.getByte()  ;// Talk flag
		reader.getByte() ;  // Rarity
		reader.getByte() ; // Appearance
		
		monster.setStrengthLevel(reader.getByte()) ; ;
		
		if(monster.isAlive()) { 
			monster.setCurrentHP(monster.getMaxHP()) ; ;
 		}
		
		return  monster;
	}
	@Override
	public Player readPlayer(NPCEntity npcEntity) {

		Player player = new Player(npcEntity);

		player.setCharScale(reader.getByte());
		player.setLevel(reader.getByte());

		reader.getByte();
		reader.getByte();

		player.setItemInventorySize(reader.getByte());

		byte itemCount = reader.getByte();

		player.setItemCount(itemCount);

		for (int i = 0; i < itemCount; i++) {

			this.sroDAO.findItem(reader.getInt())
					.map((itemEntity) -> new Equipment(new Item(itemEntity)))
					.ifPresent((eq) -> {

						if (eq.getLongId().matches("^ITEM_(([0-9A-Z]+|ROC|FORT)_)?(EU|CH).+")) {
							eq.setOptLvl(reader.getByte());

						}
						player.addItem(eq);

					});
		}

		player.setAvaterInventorySize(reader.getByte());
		byte avaterCount = reader.getByte();
		player.setAvaterItemCount(avaterCount);

		for (int i = 0; i < avaterCount; i++) {

			reader.getInt();
			reader.getByte();

		}
		;

		boolean hasMask = reader.getBoolean();

		player.setHasMask(hasMask);

		if (hasMask) {

			this.sroDAO.findNPC(reader.getInt()).ifPresent((npc) -> {
				if (npc.getLongId().startsWith("CHAR")) {
					reader.getByte();
					byte count = reader.getByte();
					for (int i = 0; i < count; i++) {
						reader.getInt();
					}
				}
			});

		}
		
		readSpawnData(player);
		readFighterData(player);
		
	
		player.setName(reader.getString());

		player.setJobType(JobType.of(reader.getByte()));
		player.setJobLvl(reader.getByte());
		player.setPvpState(PVPState.of(reader.getByte()));
		player.setHasTransport(reader.getBoolean());
		player.setInCombat(reader.getBoolean());

		if (player.isHasTransport()) {
			player.setTransportId(reader.getInt());
		}

		player.setScrollMode(reader.getByte());
		player.setInteractionMode(InteractionMode.of(reader.getByte()));
		reader.getByte();

		player.setGuildName(reader.getString());

		if (player.hasJopEquipment()) {

			player.setGuildId(reader.getInt());
			player.setGuildGrantName(reader.getString());
			player.setGuildLastCrest(reader.getInt());
			player.setUnionId(reader.getInt());
			player.setUnionLastCrest(reader.getInt());
			player.setGuildIsFriendly(reader.getBoolean());
			player.setGuildSeigeAuthority(reader.getByte());

		}

		if (player.getInteractionMode() == InteractionMode.P2N_TALK) {

			player.setStallName(reader.getString());
			player.setStallRefId(reader.getInt());

		}

		player.setEquipmentCooldown(reader.getByte());
		player.setPvpFlag(reader.getByte());

		return player;
	}

	@Override
	public Portal readPortal(PortalEntity entity) {

		Portal portal = new Portal(entity);

		readSpawnData(portal);
		
		portal.setUnk0(reader.getByte());
		portal.setUnk1(reader.getByte());
		portal.setUnk2(reader.getByte());
		portal.setUnk3(reader.getByte());

		if (portal.getUnk3() == 1) {

			portal.setUnkInt0(reader.getInt());
			portal.setUnkInt1(reader.getInt());

		} else if (portal.getUnk3() == 6) {

			portal.setOwnerName(reader.getString());
			portal.setOwnerId(reader.getInt());

		}

		if (portal.getUnk1() == 1) {
			portal.setUnkInt2(reader.getInt());
			portal.setUnk4(reader.getByte());
		}

		return portal;
	}

	@Override
	public Pet readPet(NPCEntity entity) {
		
		Pet pet = new Pet(entity) ;
		
		readSpawnData(pet);
		readFighterData(pet);
		
		byte talkFlag = reader.getByte() ; 
		
		if(talkFlag == 2) { 
			
			byte count = reader.getByte() ; 
			
			for(int i = 0 ; i < count ; i++) { 
				reader.getByte(); 	// talk option		
			}
		}
		
		
		NPCType type = pet.getType() ; 
		
		if(type == NPCType.PetPickup) { 
			
			pet.setCustomName(reader.getString()) ; 
			pet.setOwnerName(reader.getString());
			reader.getByte() ; // unkn0 
			pet.setOwnerId(reader.getInt());		
		
		}else if(type == NPCType.PetAbility) { 
			pet.setCustomName(reader.getString()) ; 
			pet.setOwnerName(reader.getString());
		
			reader.getByte() ; // jop type 
			reader.getByte() ; // murder Flag 
			pet.setOwnerId(reader.getInt()) ; 
			
		}else if(type == NPCType.PetTransport || type == NPCType.PetTransportMall) { 
			
			pet.setOwnerName(reader.getString());
			
			byte jopType = reader.getByte() ; 
			
			if(jopType == 4) { 
				reader.getByte() ;// murderFlag	
			}
			
			if(type == NPCType.PetGuildCH || type == NPCType.PetGuildEU) { 
				reader.getInt() ; // ownerRefID
			}
			
			pet.setOwnerId(reader.getInt()) ; 
			
			
		}else if(type == NPCType.PetVehicle) { 
			
			
		}else { 
			
			pet.setOwnerName(reader.getString(StandardCharsets.UTF_16LE)); 		
			reader.getByte() ; 
			reader.getByte(); 
			reader.getInt() ; 
		}
		
		
		
		
		return pet;
	}

}
