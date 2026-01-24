package org.sokybot.gameevents.internal;

import java.util.ArrayList;
import java.util.List;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.character.CharacterBuffLoadedEvent;
import org.sokybot.gameevents.events.character.CharacterItemLoadedEvent;
import org.sokybot.gameevents.events.character.CharacterLoadedEvent;
import org.sokybot.gameevents.events.character.CharacterMasteryLoadedEvent;
import org.sokybot.gameevents.events.character.CharacterSkillLoadedEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.PacketStreamReader;
import org.sokybot.persistence.service.IGameDataLookup;

public class CharacterDataEndTranslator extends AbstractTranslator {
    
    private static final int CHAR_DATA_END_OPCODE = 0x34A6;
    private static final int CHAR_DATA_BEGIN_OPCODE = 0x34A5;
    public CharacterDataEndTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return CHAR_DATA_END_OPCODE;
    }
    
    @Override
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        List<IGameEvent> events = new ArrayList<>();
        
        try {
            IStreamReader reader = packet.getStreamReader();
            parseCharacterData(machineFullName, reader, events);
            
        } catch (Exception e) {
            // Log but don't fail
        }
        return events;
    }

    private void parseCharacterData(String machineFullName, IStreamReader reader, List<IGameEvent> events) {
            reader.getInt(); // serverTime
            int refId = reader.getInt();
            reader.getByte(); // charScale
            int level = reader.getByte() & 0xFF;
            int maxLevel = reader.getByte() & 0xFF;
            long experience = reader.getLong();
            reader.getInt(); // sexpOffset
            long gold = reader.getLong();
            int skillPoints = reader.getInt();
            short statPoints = reader.getShort();
            reader.getByte(); // zerkCount
            reader.getInt(); // gatheredExpPoint
            int currentHP = reader.getInt();
            int currentMP = reader.getInt();
            reader.getByte(); // autoInvestExp
            reader.getByte(); // dailyPK
            reader.getShort(); // totalPK
            reader.getInt(); // pkPenaltyPoint
            reader.getByte(); // zerkLvl
            reader.getByte(); // freePVP
    
            reader.getByte(); // itemInventorySize
            int itemCount = reader.getByte() & 0xFF;
            int inventoryItemCount = 0;
            for (int i = 0; i < itemCount; i++) {
                CharacterItemLoadedEvent itemEvent = parseItem(machineFullName, reader, false);
                if (itemEvent != null) {
                    events.add(itemEvent);
                    inventoryItemCount++;
                }
            }
            reader.getByte(); // avatarInventorySize
            int avatarItemCount = reader.getByte() & 0xFF;
            int avatarItemsParsed = 0;
            for (int i = 0; i < avatarItemCount; i++) {
                CharacterItemLoadedEvent itemEvent = parseItem(machineFullName, reader, true);
                    avatarItemsParsed++;
            }
            reader.getBoolean(); // hasMask
            
            int masteryCount = 0;
            while (reader.getBoolean()) {
                int masteryId = reader.getInt();
                int masteryLevel = reader.getByte() & 0xFF;
                
                String masteryName = null;
                if (lookup != null) {
                    masteryName = lookup.findMasteryName(masteryId).orElse(null);
                }
                events.add(new CharacterMasteryLoadedEvent(machineFullName, masteryId, masteryName, masteryLevel));
                masteryCount++;
            }
            reader.getByte(); // mastery end byte
            
            int skillCount = 0;
            int skillRefId = reader.getInt();
            boolean isEnabled = reader.getByte() == 1;
            String skillName = null;
            int skillLevel = 0;
            var skillOpt = lookup.findSkill(skillRefId);
            if (skillOpt.isPresent()) {
                 skillName = skillOpt.get().getName();
            }
            events.add(new CharacterSkillLoadedEvent(machineFullName, skillRefId, skillName, skillLevel, isEnabled));
            skillCount++;
            
            short completedQuestCount = reader.getShort();
            for (int i = 0; i < completedQuestCount; i++) {
                reader.getInt();
            }
            int activeQuestCount = reader.getByte() & 0xFF;
            for (int i = 0; i < activeQuestCount; i++) {
                skipQuest(reader);
            }
            reader.getByte(); // unk05
            
            int collectionBookCount = reader.getInt();
            for (int i = 0; i < collectionBookCount; i++) {
                reader.getInt(); reader.getInt(); reader.getInt();
            }
            
            int uniqueId = reader.getInt();
            int xSector = reader.getUnsignedByte();
            int ySector = reader.getUnsignedByte();
            float xOffset = reader.getFloat();
            float zOffset = reader.getFloat();
            float yOffset = reader.getFloat();
            short angle = reader.getShort();
            
            boolean hasDestination = reader.getBoolean();
            reader.getByte(); // movementType
            if (hasDestination) {
                int destXSector = reader.getUnsignedByte();
                int destYSector = reader.getUnsignedByte();
                if (destYSector == 0x80) {
                    reader.getInt(); reader.getInt(); reader.getInt();
                } else {
                    reader.getShort(); reader.getShort(); reader.getShort();
                }
            } else {
                reader.getByte(); reader.getShort();
            }
            
            byte lifeState = reader.getByte();
            byte debuffStatus = reader.getByte();
            byte motionState = reader.getByte();
            byte characterStatus = reader.getByte();
            float walkSpeed = reader.getFloat();
            float runSpeed = reader.getFloat();
            reader.getFloat(); // hwanSpeed
            
            int activeBuffCount = reader.getByte() & 0xFF;
            for (int i = 0; i < activeBuffCount; i++) {
                int buffRefId = reader.getInt();
                int duration = reader.getInt();
                String buffName = null;
                buffName = lookup.findSkill(buffRefId)
                        .map(skill -> skill.getName())
                        .orElse(null);
                events.add(new CharacterBuffLoadedEvent(machineFullName, buffRefId, buffName, duration, false));
            }
            
            String characterName = readString(reader);
            String jobName = readString(reader);
            byte jobType = reader.getByte();
            byte jobLevel = reader.getByte();
            int jobExp = reader.getInt();
            reader.getInt(); // jobContribution
            reader.getInt(); // jobReward
            
            byte pvpState = reader.getByte();
            boolean hasTransport = reader.getBoolean();
            boolean inCombat = reader.getBoolean();
            if (hasTransport) {
                reader.getInt(); // transportId
            }
            reader.getByte(); // pvpFlag
            reader.getLong(); // guideFlag
            reader.getInt(); // accountId
            reader.getByte(); // gmFlag
            reader.getByte(); // activationFlag
            
            int hotKeyCount = reader.getByte() & 0xFF;
            for (int i = 0; i < hotKeyCount; i++) {
                reader.getByte(); reader.getByte(); reader.getInt();
            }
            
            events.add(new CharacterLoadedEvent(machineFullName, uniqueId, refId,
                characterName, level, maxLevel, experience, gold, skillPoints, statPoints,
                currentHP, currentMP, lifeState, debuffStatus, motionState, characterStatus,
                walkSpeed, runSpeed, xSector, ySector, xOffset, yOffset, zOffset, angle,
                inventoryItemCount + avatarItemsParsed, avatarItemsParsed, masteryCount, skillCount, activeBuffCount, hotKeyCount,
                jobName, jobType, jobLevel, jobExp, pvpState, hasTransport, inCombat));
    }

    private CharacterItemLoadedEvent parseItem(String machineFullName, IStreamReader reader, boolean isEquipped) {
            byte slot = reader.getByte();
            int rentType = reader.getInt();
            switch (rentType) {
                case 1: reader.getShort(); reader.getInt(); reader.getInt(); break;
                case 2: reader.getShort(); reader.getShort(); reader.getInt(); break;
                case 3: reader.getShort(); reader.getInt(); reader.getInt(); reader.getShort(); reader.getInt(); break;
            }
            int itemId = reader.getInt();
            String itemName = null;
            if (lookup != null) {
                itemName = lookup.findItem(itemId)
                    .map(item -> item.getName())
                    .orElse(null);
            }
            return new CharacterItemLoadedEvent(machineFullName, slot, itemId, itemName, 1, 0, isEquipped);
    }

    private void skipQuest(IStreamReader reader) {
        reader.getInt(); // questId
        reader.getByte(); // achievementCount
        reader.getByte(); // requiresSharePt
        byte type = reader.getByte();
        if (type == 28) reader.getInt();
        reader.getByte(); // status
        if (type != 8) {
            int objCount = reader.getByte() & 0xFF;
            for (int i = 0; i < objCount; i++) {
                reader.getByte(); reader.getByte();
                int nameLen = reader.getShort() & 0xFFFF;
                reader.getBytes(nameLen);
                int taskCount = reader.getByte() & 0xFF;
                for (int j = 0; j < taskCount; j++) reader.getInt();
            }
        }
        if (type == 88) {
            int taskCount = reader.getByte() & 0xFF;
            for (int i = 0; i < taskCount; i++) reader.getInt();
        }
    }

    private String readString(IStreamReader reader) {
        int length = reader.getShort() & 0xFFFF;
        return new String(reader.getBytes(length));
    }
}
