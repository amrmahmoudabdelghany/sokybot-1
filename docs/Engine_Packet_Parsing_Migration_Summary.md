# Engine Packet Parsing Migration Summary

## Status: IN PROGRESS

## Overview
Migrating engine from packet parsing to event-driven architecture. The engine should receive domain events from `sokybot-game-events` via OSGi EventAdmin, not parse packets directly.

## Completed Steps

### ✅ Phase 1: Infrastructure Setup
1. **Created backup structure** - All files moved to `.backup/` instead of deleted
2. **Enabled PacketDispatcher** - Uncommented and activated packet forwarding to GameEventPublisher
3. **Added dependencies** - Added `sokybot-game-events` as provided dependency in engine's pom.xml
4. **Moved packet parsing infrastructure to backup**:
   - `PacketListenerInstaller.java` → `.backup/sokybot-engine/src/main/java/org/sokybot/machine/`
   - `PacketListener.java` → `.backup/sokybot-engine/src/main/java/org/sokybot/machine/network/`
   - `PacketListenerAdapter.java` → `.backup/sokybot-engine/src/main/java/org/sokybot/machine/network/`
5. **Fixed translator opcode** - Fixed `MasteryLevelUpTranslator` opcode from 0x70B5 to 0xB0A2

### ✅ Phase 2: Translators Verification
**Existing translators verified:**
- ✅ `CharacterLoadedTranslator` - Handles CHAR_DATA (0x3013)
- ✅ `MasteryLevelUpTranslator` - Handles CHAR_MASTERY_LVL_UP (0xB0A2) - FIXED opcode
- ✅ `SkillLevelUpTranslator` - Handles CHAR_SKILL_LVL_UP (0xB0A1)
- ✅ `ChatMessageTranslator` - Handles CHAT_UPDATE (0x3026) - Server packets only

**Note:** CommandHandler listens to CHAT_REQUEST (client packet 0x7025), which is different from server chat messages.

## Remaining Work

### ✅ Phase 3: Controller Migration

#### CommandHandler ✅ COMPLETED
- **Original**: `@PacketListener(opcode = ClientOpcode.CHAT_REQUEST)` - Handles CLIENT packet (outgoing)
- **Migrated**: Now subscribes to `ChatMessageEvent` via OSGi EventAdmin
- **Changes**:
  - Removed `@PacketListener` annotation
  - Added OSGi `EventHandler` registration in `@PostConstruct`
  - Created `handleChatMessageEvent()` method to process OSGi events
  - Registered for topic: `sokybot/game/*/ChatMessageEvent`
  - Filters for private messages and executes commands
- **Note**: Original code listened to CLIENT packets but logic suggests it should handle SERVER packets (incoming chat), which is now fixed

#### EnvironmentHandler (12 packet listeners)
**Migrations needed:**
1. `onGroupSpawnBegin()` → Subscribe to `GroupSpawnBeginEvent`
2. `onGroupSpawn()` → Subscribe to `GroupSpawnBeginEvent` + handle spawn batch
3. `onGroupSpawnEnd()` → Subscribe to `GroupSpawnEndEvent`
4. `onSingleSpawn()` → Subscribe to `EntitySpawnEvent`
5. `onSingleDespawn()` → Subscribe to `EntityDespawnEvent`
6. `onSkillCastStarted()` → Subscribe to `SkillCastEvent`
7. `onSkillCastEnd()` → Subscribe to `SkillCastEndEvent`
8. `onHPMPUpdate()` → Subscribe to `EntityHPMPUpdateEvent`
9. `onSpeedUpdate()` → Subscribe to `EntitySpeedUpdateEvent`
10. `onAngleUpdate()` → Subscribe to `EntityAngleUpdateEvent`
11. `onSpawnSelected()` → Subscribe to `EntitySelectedEvent`
12. `onSpawnMovement()` → Subscribe to `EntityMovementEvent`
13. `onSpawnStuck()` → Subscribe to `EntityStoppedEvent`

#### TrainerHandler (9 packet listeners)
**Migrations needed:**
1. `parsingCharData()` → Subscribe to `CharacterLoadedEvent`
2. `parsingCharInfo()` → Subscribe to `CharacterInfoEvent`
3. `expSPUpdate()` → Subscribe to `ExpUpdateEvent` + `SkillPointsUpdateEvent`
4. `onCharDie()` → Subscribe to `CharacterDeathEvent`
5. `attackGainsUpdates()` → Subscribe to `GoldUpdateEvent`
6. `masteryLevelUp()` → Subscribe to `MasteryLevelUpEvent`
7. `userlvlUpSkill()` → Subscribe to `SkillLevelUpEvent` (client packet - needs special handling)
8. `skillLevelUp()` → Subscribe to `SkillLevelUpEvent`

### ⏳ Phase 4: Spring Event Cleanup
**Files moved to backup:**
- `machine/event/monsterevent/*` - All monster events (replaced by entity events)
- `machine/event/trainerevent/TrainerAttackedEvent.java` - Use combat events instead
- `machine/event/SkillCastStartEvent.java` - Use `SkillCastEvent` instead
- `machine/event/SkillCastErrorEevent.java` - Handle via error events

**Files kept (engine-internal):**
- `TrainerLoadedEvent` - Could migrate to OSGi or keep as Spring event
- `UserConfigUpdatedEvent` - UI config, could migrate to OSGi
- `TrainerStuckEvent` - Engine-internal logic
- `TrainerReachDestinationEvent` - Engine-internal logic

## Architecture

### Event Flow
```
Proxy → IPacketPublisher → PacketDispatcher (Spring) → 
  GameEventPublisher (OSGi) → IPacketTranslator → 
  OSGi EventAdmin → Engine Controllers (OSGi EventHandler)
```

### Key Changes
1. **Packet Parsing** - Moved from engine to `sokybot-game-events` bundle
2. **Event Subscription** - Controllers subscribe to OSGi EventAdmin events
3. **Loose Coupling** - Engine receives domain events, not raw packets
4. **Reusability** - Events available to all bundles (UI, plugins, etc.)

## Next Steps
1. Migrate EnvironmentHandler to OSGi EventHandler
2. Migrate TrainerHandler to OSGi EventHandler
3. Handle CommandHandler client packet scenario
4. Remove @PacketListener annotations from controllers
5. Test event flow end-to-end
