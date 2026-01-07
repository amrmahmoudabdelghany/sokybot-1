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
        
        // Create shared ChunkedPacketManager for translators that need it
        ChunkedPacketManager chunkManager = new ChunkedPacketManager();
        
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
        
        // Character Events - using CharacterDataEndTranslator for fine-grained parsing
        // Note: CharacterLoadedTranslator is replaced by CharacterDataEndTranslator
        // which emits multiple fine-grained events plus the summary CharacterLoadedEvent
        CharacterDataBeginTranslator beginTranslator = new CharacterDataBeginTranslator(lookup);
        CharacterDataEndTranslator endTranslator = new CharacterDataEndTranslator(lookup);
        beginTranslator.setChunkManager(chunkManager);
        endTranslator.setChunkManager(chunkManager);
        
        translators.put(0x34A5, beginTranslator);  // CHAR_DATA_BEGIN
        translators.put(0x34A6, endTranslator);    // CHAR_DATA_END - emits fine-grained events
        translators.put(0x3013, new CharacterLoadedTranslator(lookup)); // Direct parsing fallback
        translators.put(0x303D, new CharacterInfoTranslator(lookup));
        
        // Character Data (0x3013) is also handled by CharacterLoadedTranslator above
        // for backward compatibility when not in chunked mode
        
        // Authentication Events
        translators.put(0xA101, new AgentListTranslator(lookup));
        translators.put(0xA102, new LoginResponseTranslator(lookup));
        
        // Social Events
        translators.put(0x3026, new ChatMessageTranslator(lookup));
        
        // Entity State Updates
        translators.put(0x30BF, new LifeStateUpdateTranslator(lookup));
        
        // Inventory Events
        translators.put(0x3040, new InventoryItemUpdateTranslator(lookup));
        translators.put(0xB034, new InventoryOperationTranslator(lookup));
        
        // Party Events
        translators.put(0x3864, new PartyUpdateTranslator(lookup));
        
        // Storage Events
        translators.put(0x3047, new StorageOpenTranslator(lookup, 0x3047, (byte)0)); // Personal
        translators.put(0x3253, new StorageOpenTranslator(lookup, 0x3253, (byte)1)); // Guild
        
        // Quest Events
        translators.put(0x30D5, new QuestUpdateTranslator(lookup));
        
        // Action Events
        translators.put(0xB04B, new EntityDeselectedTranslator(lookup));
        translators.put(0xB046, new NpcTalkTranslator(lookup));
        
        // Session Events
        translators.put(0x300A, new LogoutTranslator(lookup));
        translators.put(0x34B5, new TeleportCompleteTranslator(lookup));
        
        // Exchange/Trade Events
        translators.put(0x3085, new ExchangeStartedTranslator(lookup));
        translators.put(0x3088, new ExchangeCancelledTranslator(lookup));
        
        // Group Spawn Events
        translators.put(0x3018, new GroupSpawnEndTranslator(lookup));
        
        // Pet/Mount Events
        translators.put(0xB0CB, new MountStateUpdateTranslator(lookup));
        
        // COS (Controlled Object System) Events
        translators.put(0x30C8, new CosDataTranslator(lookup));     // COS spawn data
        translators.put(0x30C9, new CosUpdateTranslator(lookup));   // COS state updates
        translators.put(0x3422, new FellowStatUpdateTranslator(lookup)); // Fellow stats
        
        // Job Events
        translators.put(0xB0E1, new JobJoinTranslator(lookup));
        translators.put(0xB0E2, new JobLeaveTranslator(lookup));
        
        // Alchemy Events
        translators.put(0xB150, new AlchemyResultTranslator(lookup, 0xB150, (byte)1)); // Elixir
        translators.put(0xB151, new AlchemyResultTranslator(lookup, 0xB151, (byte)2)); // Stone
        
        // Additional Party Events
        translators.put(0x3080, new PartyInviteTranslator(lookup));
        
        // Additional Inventory Events
        translators.put(0xB04C, new ItemUseTranslator(lookup));
        translators.put(0x3052, new ItemDurabilityUpdateTranslator(lookup));
        translators.put(0x3092, new InventorySizeUpdateTranslator(lookup));
        
        System.out.println("GAME-EVENTS: Created " + translators.size() + " translators for game: " + lookup.getGamePath());
        return translators;
    }
}
