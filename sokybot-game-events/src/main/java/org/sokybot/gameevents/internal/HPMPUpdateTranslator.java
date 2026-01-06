package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.EntityHPMPUpdateEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates HP/MP update packets (opcode 0x3057) to EntityHPMPUpdateEvent.
 * Based on EnvironmentHandler.onHPMPUpdate() pattern.
 */
@Component(service = IPacketTranslator.class)
public class HPMPUpdateTranslator implements IPacketTranslator {
    
    private static final int HPMP_UPDATE_OPCODE = 0x3057;
    
    @Override
    public int getOpcode() {
        return HPMP_UPDATE_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int entityId = reader.getInt();
            reader.getShort(); // Skip unknown short
            byte changeTypeByte = reader.getByte();
            
            EntityHPMPUpdateEvent.ChangeType changeType = mapChangeType(changeTypeByte);
            Integer newHP = null;
            Integer newMP = null;
            Integer badStatus = null;
            
            switch (changeType) {
                case HP_CHANGED:
                    newHP = reader.getInt();
                    break;
                case MP_CHANGED:
                    newMP = reader.getInt();
                    break;
                case HP_AND_MP_CHANGED:
                    newHP = reader.getInt();
                    newMP = reader.getInt();
                    break;
                case BAD_STATUS:
                    badStatus = reader.getInt();
                    break;
                case HP_AND_BAD_STATUS:
                    newHP = reader.getInt();
                    badStatus = reader.getInt();
                    break;
                case MP_AND_BAD_STATUS:
                    newMP = reader.getInt();
                    badStatus = reader.getInt();
                    break;
            }
            
            return new EntityHPMPUpdateEvent(machineFullName, entityId, changeType, 
                                            newHP, newMP, badStatus);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private EntityHPMPUpdateEvent.ChangeType mapChangeType(byte value) {
        // Based on HealthChange enum from engine
        switch (value) {
            case 1: return EntityHPMPUpdateEvent.ChangeType.HP_CHANGED;
            case 2: return EntityHPMPUpdateEvent.ChangeType.MP_CHANGED;
            case 3: return EntityHPMPUpdateEvent.ChangeType.HP_AND_MP_CHANGED;
            case 4: return EntityHPMPUpdateEvent.ChangeType.BAD_STATUS;
            case 5: return EntityHPMPUpdateEvent.ChangeType.HP_AND_BAD_STATUS;
            case 6: return EntityHPMPUpdateEvent.ChangeType.MP_AND_BAD_STATUS;
            default: return EntityHPMPUpdateEvent.ChangeType.HP_CHANGED;
        }
    }
}
