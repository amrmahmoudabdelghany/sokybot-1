package org.sokybot.gameevents;

import java.util.Set;
import java.util.HashSet;

import org.osgi.service.component.annotations.Component;
import org.sokybot.gameevents.events.core.ITranslatorProvider;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.gameevents.internal.*;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Core translator provider for standard packets.
 * Provides all built-in translators with highest priority (100).
 * 
 * <p>This provider wraps all existing translators from TranslatorFactoryImpl,
 * making them available through the extensible provider system.
 */
@Component(
    service = ITranslatorProvider.class,
    property = {"priority=100", "type=core"}
)
public class CoreTranslatorProvider implements ITranslatorProvider {
    
    // All core opcodes supported by this provider
    private static final Set<Integer> CORE_OPCODES = Set.of(
        // Entity Events
        0x3015, 0x3016, 0x3019, 0xB024, 0xB021, 0xB023, 0x30D0, 0xB045,
        // Combat Events
        0xB070, 0xB071, 0xB074, 0x3057, 0x3011, 0xB0A7,
        // Buff Events
        0x30BD, 0x30BE,
        // Progression Events
        0x3056, 0x304E, 0x3843, 0xB0A2, 0xB0A1,
        // Character Events
        0x34A5, 0x34A6, 0x3013, 0x303D,
        // Authentication Events
        0x6102, 0xA101, 0xA102, 0xA103,
        // Social Events
        0x3026,
        // Entity State Updates
        0x30BF,
        // Inventory Events
        0x3040, 0xB034,
        // Party Events
        0x3864,
        // Storage Events
        0x3047, 0x3253,
        // Quest Events
        0x30D5,
        // Action Events
        0xB04B, 0xB046,
        // Session Events
        0x300A, 0x34B5,
        // Exchange/Trade Events
        0x3085, 0x3088,
        // Group Spawn Events
        0x3018,
        // Pet/Mount Events
        0xB0CB,
        // COS (Controlled Object System) Events
        0x30C8, 0x30C9, 0x3422,
        // Job Events
        0xB0E1, 0xB0E2,
        // Alchemy Events
        0xB150, 0xB151,
        // Additional Party Events
        0x3080,
        // Additional Inventory Events
        0xB04C, 0x3052, 0x3092,
        // Visual/Animation Events
        0x3091, 0x3036, 0x3038, 0x3039, 0x3058,
        // Stat Update Events
        0xB050, 0xB051, 0x30DF, 0x3200,
        // Extended Exchange Events
        0x3086, 0x3087, 0x3089,
        // Stall (Street Vendor) Events
        0x30B7, 0x30B8, 0x30B9, 0x30BB,
        // Party Matching Events
        0x306E, 0x3065, 0xB067,
        // Entity/Character Events
        0x3054, 0x304D,
        // World Events
        0x3020, 0x3809, 0x3077,
        // Quest Events
        0xB0D9,
        // Skill Events
        0xB202,
        // Alchemy Events
        0x34AA,
        // Social Events
        0x302D, 0x3101,
        // Item Events
        0xB03E,
        // Item Perk/Buff Events
        0x325F, 0x3261,
        // Job System Events
        0xB0E3, 0x30E6,
        // Combat/Equipment Events
        0x3201,
        // Teleport Events
        0xB05A,
        // Storage Events
        0xB558
    );
    
    @Override
    public boolean supports(int opcode, IGameDataLookup lookup) {
        return CORE_OPCODES.contains(opcode);
    }
    
    @Override
    public Set<Integer> getSupportedOpcodes() {
        return CORE_OPCODES;
    }
    
    @Override
    public IPacketTranslator createTranslator(int opcode, IGameDataLookup lookup) {
        // Create core translators (without chunk manager - passed via translate parameter)
        switch (opcode) {
            // Entity Events
            case 0x3015: return new EntitySpawnTranslator(lookup);
            case 0x3016: return new EntityDespawnTranslator(lookup);
            case 0x3019: return new GroupSpawnBeginTranslator(lookup);
            case 0xB024: return new EntityAngleUpdateTranslator(lookup);
            case 0xB021: return new EntityMovementTranslator(lookup);
            case 0xB023: return new EntityStoppedTranslator(lookup);
            case 0x30D0: return new SpeedUpdateTranslator(lookup);
            case 0xB045: return new EntitySelectedTranslator(lookup);
            
            // Combat Events
            case 0xB070: return new SkillCastTranslator(lookup);
            case 0xB071: return new SkillCastEndTranslator(lookup);
            case 0xB074: return new SkillCastConfirmTranslator(lookup);
            case 0x3057: return new HPMPUpdateTranslator(lookup);
            case 0x3011: return new CharacterDeathTranslator(lookup);
            case 0xB0A7: return new BerserkConfirmTranslator(lookup);
            
            // Buff Events
            case 0x30BD: return new BuffAppliedTranslator(lookup);
            case 0x30BE: return new BuffRemovedTranslator(lookup);
            
            // Progression Events
            case 0x3056: return new ExpUpdateTranslator(lookup);
            case 0x304E: return new GoldUpdateTranslator(lookup);
            case 0x3843: return new SkillPointsUpdateTranslator(lookup);
            case 0xB0A2: return new MasteryLevelUpTranslator(lookup);
            case 0xB0A1: return new SkillLevelUpTranslator(lookup);
            
            // Character Events
            case 0x34A5: return new CharacterDataBeginTranslator(lookup);
            case 0x34A6: return new CharacterDataEndTranslator(lookup);
            case 0x3013: return new CharacterLoadedTranslator(lookup);
            case 0x303D: return new CharacterInfoTranslator(lookup);
            
            // Authentication Events
            case 0x6102: return new LoginRequestTranslator(lookup);
            case 0xA101: return new AgentListTranslator(lookup);
            case 0xA102: return new LoginResponseTranslator(lookup);
            case 0xA103: return new AuthResponseTranslator(lookup);
            
            // Social Events
            case 0x3026: return new ChatMessageTranslator(lookup);
            
            // Entity State Updates
            case 0x30BF: return new LifeStateUpdateTranslator(lookup);
            
            // Inventory Events
            case 0x3040: return new InventoryItemUpdateTranslator(lookup);
            case 0xB034: return new InventoryOperationTranslator(lookup);
            
            // Party Events
            case 0x3864: return new PartyUpdateTranslator(lookup);
            
            // Storage Events
            case 0x3047: return new StorageOpenTranslator(lookup, 0x3047, (byte)0);
            case 0x3253: return new StorageOpenTranslator(lookup, 0x3253, (byte)1);
            
            // Quest Events
            case 0x30D5: return new QuestUpdateTranslator(lookup);
            
            // Action Events
            case 0xB04B: return new EntityDeselectedTranslator(lookup);
            case 0xB046: return new NpcTalkTranslator(lookup);
            
            // Session Events
            case 0x300A: return new LogoutTranslator(lookup);
            case 0x34B5: return new TeleportCompleteTranslator(lookup);
            
            // Exchange/Trade Events
            case 0x3085: return new ExchangeStartedTranslator(lookup);
            case 0x3088: return new ExchangeCancelledTranslator(lookup);
            
            // Group Spawn Events
            case 0x3018: return new GroupSpawnEndTranslator(lookup);
            
            // Pet/Mount Events
            case 0xB0CB: return new MountStateUpdateTranslator(lookup);
            
            // COS Events
            case 0x30C8: return new CosDataTranslator(lookup);
            case 0x30C9: return new CosUpdateTranslator(lookup);
            case 0x3422: return new FellowStatUpdateTranslator(lookup);
            
            // Job Events
            case 0xB0E1: return new JobJoinTranslator(lookup);
            case 0xB0E2: return new JobLeaveTranslator(lookup);
            
            // Alchemy Events
            case 0xB150: return new AlchemyResultTranslator(lookup, 0xB150, (byte)1);
            case 0xB151: return new AlchemyResultTranslator(lookup, 0xB151, (byte)2);
            
            // Additional Party Events
            case 0x3080: return new PartyInviteTranslator(lookup);
            
            // Additional Inventory Events
            case 0xB04C: return new ItemUseTranslator(lookup);
            case 0x3052: return new ItemDurabilityUpdateTranslator(lookup);
            case 0x3092: return new InventorySizeUpdateTranslator(lookup);
            
            // Visual/Animation Events
            case 0x3091: return new EmotionTranslator(lookup);
            case 0x3036: return new PickupAnimationTranslator(lookup);
            case 0x3038: return new EquipItemTranslator(lookup);
            case 0x3039: return new UnequipItemTranslator(lookup);
            case 0x3058: return new DamageEffectTranslator(lookup);
            
            // Stat Update Events
            case 0xB050: return StatPointsUpdateTranslator.forStrength(lookup);
            case 0xB051: return StatPointsUpdateTranslator.forIntelligence(lookup);
            case 0x30DF: return new HwanLevelUpdateTranslator(lookup);
            case 0x3200: return new AttackSpeedUpdateTranslator(lookup);
            
            // Extended Exchange Events
            case 0x3086: return new ExchangeConfirmedTranslator(lookup);
            case 0x3087: return new ExchangeApprovedTranslator(lookup);
            case 0x3089: return new ExchangeUpdateTranslator(lookup);
            
            // Stall Events
            case 0x30B7: return StallEventTranslator.forAction(lookup);
            case 0x30B8: return StallEventTranslator.forCreated(lookup);
            case 0x30B9: return StallEventTranslator.forDestroyed(lookup);
            case 0x30BB: return StallEventTranslator.forNameChanged(lookup);
            
            // Party Matching Events
            case 0x306E: return PartyMatchingTranslator.forPlayerJoinRequest(lookup);
            case 0x3065: return PartyMatchingTranslator.forPartyCreated(lookup);
            case 0xB067: return PartyMatchingTranslator.forMemberCountUpdate(lookup);
            
            // Entity/Character Events
            case 0x3054: return new LevelUpTranslator(lookup);
            case 0x304D: return new ItemOwnershipRemovedTranslator(lookup);
            
            // World Events
            case 0x3020: return new CelestialPositionTranslator(lookup);
            case 0x3809: return new WeatherUpdateTranslator(lookup);
            case 0x3077: return new GameReadyTranslator(lookup);
            
            // Quest Events
            case 0xB0D9: return new QuestAbandonTranslator(lookup);
            
            // Skill Events
            case 0xB202: return new SkillWithdrawTranslator(lookup);
            
            // Alchemy Events
            case 0x34AA: return new MagicOptionUpdateTranslator(lookup);
            
            // Social Events
            case 0x302D: return new ChatRestrictTranslator(lookup);
            case 0x3101: return new GuildInfoTranslator(lookup);
            
            // Item Events
            case 0xB03E: return new ItemRepairTranslator(lookup);
            
            // Item Perk/Buff Events
            case 0x325F: return new ItemPerkAddTranslator(lookup);
            case 0x3261: return new ItemPerkRemoveTranslator(lookup);
            
            // Job System Events
            case 0xB0E3: return new JobAliasUpdateTranslator(lookup);
            case 0x30E6: return new JobExperienceUpdateTranslator(lookup);
            
            // Combat/Equipment Events
            case 0x3201: return new AmmoUpdateTranslator(lookup);
            
            // Teleport Events
            case 0xB05A: return new TeleportResponseTranslator(lookup);
            
            // Storage Events
            case 0xB558: return new StorageBoxTakeItemTranslator(lookup);
            
            default:
                return null;
        }
    }
    
    @Override
    public int getPriority() {
        return 100; // Highest priority
    }
}
