package org.sokybot.gameevents;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.api.events.ITranslatorFactory;
import org.sokybot.gameevents.internal.*;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Factory implementation that creates all translator instances.
 * Each game gets its own set of translator instances with game-specific lookup.
 * Published as OSGi service for PacketDispatcher to use.
 */
@Component(service = ITranslatorFactory.class)
public class TranslatorFactoryImpl implements ITranslatorFactory {
    
    @Override
    public Map<Integer, IPacketTranslator> createTranslators(IGameDataLookup lookup) {
        Map<Integer, IPacketTranslator> translators = new HashMap<>();
        
        // Entity Events
        translators.put(0x3015, new EntitySpawnTranslator(lookup));
        translators.put(0x3016, new EntityDespawnTranslator(lookup));
        translators.put(0x3019, new GroupSpawnBeginTranslator(lookup));
        translators.put(0xB024, new EntityAngleUpdateTranslator(lookup));
        translators.put(0xB021, new EntityMovementTranslator(lookup));
        translators.put(0xB023, new EntityStoppedTranslator(lookup));
        translators.put(0x30D0, new SpeedUpdateTranslator(lookup));
        translators.put(0xB045, new EntitySelectedTranslator(lookup));
        
        // Combat Events
        translators.put(0xB070, new SkillCastTranslator(lookup));
        translators.put(0xB071, new SkillCastEndTranslator(lookup));
        translators.put(0xB074, new SkillCastConfirmTranslator(lookup));
        translators.put(0x3057, new HPMPUpdateTranslator(lookup));
        translators.put(0x3011, new CharacterDeathTranslator(lookup));
        translators.put(0xB0A7, new BerserkConfirmTranslator(lookup));
        
        // Buff Events
        translators.put(0x30BD, new BuffAppliedTranslator(lookup));
        translators.put(0x30BE, new BuffRemovedTranslator(lookup));
        
        // Progression Events
        translators.put(0x3056, new ExpUpdateTranslator(lookup));
        translators.put(0x304E, new GoldUpdateTranslator(lookup));
        translators.put(0x3843, new SkillPointsUpdateTranslator(lookup));
        translators.put(0xB0A2, new MasteryLevelUpTranslator(lookup));
        translators.put(0xB0A1, new SkillLevelUpTranslator(lookup));
        
        // Character Events
        translators.put(0x3013, new CharacterLoadedTranslator(lookup));
        translators.put(0x303D, new CharacterInfoTranslator(lookup));
        
        // Authentication Events
        translators.put(0xA101, new AgentListTranslator(lookup));
        translators.put(0xA102, new LoginResponseTranslator(lookup));
        
        // Social Events
        translators.put(0x3026, new ChatMessageTranslator(lookup));
        
        System.out.println("GAME-EVENTS: Created " + translators.size() + " translators for game: " + lookup.getGamePath());
        return translators;
    }
}
